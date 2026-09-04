package com.chesscoach.backend.analysis.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wraps ONE running Stockfish process and speaks UCI protocol over its stdin/stdout.
 *
 * NOT thread-safe by design — exactly one caller (the pool/worker) should use a
 * given instance at a time. Concurrency across multiple games is handled by
 * running multiple StockfishEngine instances (StockfishEnginePool, next step),
 * not by sharing one instance across threads.
 */
public class StockfishEngine implements AutoCloseable {

    private static final Pattern SCORE_PATTERN =
            Pattern.compile("score (cp|mate) (-?\\d+)");

    private final String executablePath;
    private Process process;
    private BufferedWriter stdin;
    private BlockingQueue<String> outputLines;
    private Thread readerThread;

    public StockfishEngine(String executablePath) {
        this.executablePath = executablePath;
    }

    /**
     * Starts the process and performs the standard UCI handshake:
     * "uci" -> wait for "uciok", then "isready" -> wait for "readyok".
     * Throws EngineException if the process fails to start or doesn't
     * respond correctly within the timeout — a hung/broken engine must
     * fail loudly here, not silently produce garbage evaluations later.
     */
    public void start() {
        try {
            ProcessBuilder builder = new ProcessBuilder(executablePath);
            builder.redirectErrorStream(true); // merge stderr into stdout — one stream to read
            process = builder.start();
        } catch (IOException e) {
            throw new EngineException("Failed to start Stockfish process at: " + executablePath, e);
        }

        outputLines = new LinkedBlockingQueue<>();
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        BufferedReader stdout = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

        // Reading a process's stdout must happen on its own thread — if we tried
        // to read and write from the same thread, both sides could deadlock
        // waiting on full OS pipe buffers. This thread's only job is to move
        // lines from the process into the queue as they arrive.
        readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = stdout.readLine()) != null) {
                    outputLines.put(line);
                }
            } catch (IOException | InterruptedException ignored) {
                // Process closed or thread interrupted during shutdown — expected, not an error.
            }
        }, "stockfish-reader");
        readerThread.setDaemon(true);
        readerThread.start();

        sendCommand("uci");
        readUntil(line -> line.equals("uciok"), line -> {}, Duration.ofSeconds(10));

        sendCommand("isready");
        readUntil(line -> line.equals("readyok"), line -> {}, Duration.ofSeconds(10));
    }

    /**
     * Evaluates a single position to a fixed depth and returns the engine's
     * best move plus its evaluation of that position.
     *
     * depthLimit is intentionally required, not optional — an unbounded
     * "go" with no depth/time limit can run indefinitely, and that's
     * exactly the failure mode that would silently hang a worker thread
     * forever. We always bound the search explicitly.
     */
    public EngineEvaluation evaluate(String fen, int depthLimit) {
        if (!isAlive()) {
            throw new EngineException("Cannot evaluate: engine process is not running (crashed or not started).");
        }

        sendCommand("position fen " + fen);
        sendCommand("go depth " + depthLimit);

        int[] lastScore = {0};       // [0]=type(0=none,1=cp,2=mate), [1]=value — simple mutable capture for the lambda
        int[] scoreValue = {0};

        String bestMoveLine = readUntil(
                line -> line.startsWith("bestmove"),
                line -> {
                    Matcher m = SCORE_PATTERN.matcher(line);
                    if (m.find()) {
                        lastScore[0] = m.group(1).equals("mate") ? 2 : 1;
                        scoreValue[0] = Integer.parseInt(m.group(2));
                    }
                },
                Duration.ofSeconds(30) // hard cap: depth-limited search should never legitimately take this long
        );

        String[] parts = bestMoveLine.split("\\s+");
        String bestMove = parts.length >= 2 ? parts[1] : null;
        if (bestMove == null || bestMove.equals("(none)")) {
            throw new EngineException("Stockfish returned no legal move for FEN: " + fen
                    + " — position may be checkmate/stalemate, which should be filtered before calling evaluate().");
        }

        Integer cp = lastScore[0] == 1 ? scoreValue[0] : null;
        Integer mate = lastScore[0] == 2 ? scoreValue[0] : null;
        return new EngineEvaluation(bestMove, cp, mate);
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    private void sendCommand(String command) {
        if (!isAlive() && process != null) {
            throw new EngineException("Cannot send command \"" + command + "\": engine process has died.");
        }
        try {
            stdin.write(command);
            stdin.newLine();
            stdin.flush();
        } catch (IOException e) {
            throw new EngineException("Failed to send command to Stockfish: " + command, e);
        }
    }

    /**
     * Drains lines from the queue, passing each to lineHandler, until stopCondition
     * matches or the timeout elapses. Returns the matching (final) line.
     *
     * This is the core piece that makes the wrapper safe to use inside a web app:
     * BufferedReader.readLine() alone blocks forever with no way to time out.
     * Polling a BlockingQueue with a deadline gives us a hard upper bound on
     * how long any single call can hang, no matter what the process does.
     */
    private String readUntil(Predicate<String> stopCondition, Consumer<String> lineHandler, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (true) {
            long remainingMs = Duration.between(Instant.now(), deadline).toMillis();
            if (remainingMs <= 0) {
                throw new EngineException("Timed out after " + timeout.getSeconds()
                        + "s waiting for Stockfish response.");
            }
            String line;
            try {
                line = outputLines.poll(remainingMs, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new EngineException("Interrupted while waiting for Stockfish response.", e);
            }
            if (line == null) {
                continue; // poll timed out this iteration but overall deadline not yet reached; loop re-checks deadline
            }
            lineHandler.accept(line);
            if (stopCondition.test(line)) {
                return line;
            }
        }
    }

    /**
     * Graceful shutdown: ask the engine to quit via UCI protocol first,
     * give it a moment to exit cleanly, then force-kill if it didn't.
     * Never leave a Stockfish process running after this returns — an
     * orphaned engine process is a real resource leak under load.
     */
    @Override
    public void close() {
        if (process == null) return;
        try {
            if (isAlive()) {
                sendCommand("quit");
                boolean exited = process.waitFor(3, TimeUnit.SECONDS);
                if (!exited) {
                    process.destroyForcibly();
                }
            }
        } catch (Exception e) {
            process.destroyForcibly();
        } finally {
            readerThread.interrupt();
        }
    }
}