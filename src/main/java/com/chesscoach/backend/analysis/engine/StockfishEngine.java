package com.chesscoach.backend.analysis.engine;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
 * running multiple StockfishEngine instances (StockfishEnginePool), not by
 * sharing one instance across threads.
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

    public void start() {
        try {
            ProcessBuilder builder = new ProcessBuilder(executablePath);
            builder.redirectErrorStream(true);
            process = builder.start();
        } catch (IOException e) {
            throw new EngineException("Failed to start Stockfish process at: " + executablePath, e);
        }

        outputLines = new LinkedBlockingQueue<>();
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));

        BufferedReader stdout = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

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

    public EngineEvaluation evaluate(String fen, int depthLimit) {
        if (!isAlive()) {
            throw new EngineException("Cannot evaluate: engine process is not running (crashed or not started).");
        }

        // Defensive reset: this engine instance is pooled and shared. If a
        // prior caller left MultiPV elevated (via evaluateTopMoves), this
        // call would otherwise misparse multi-line output. Never trust
        // prior state on a shared instance — always set it explicitly.
        sendCommand("setoption name MultiPV value 1");
        sendCommand("position fen " + fen);
        sendCommand("go depth " + depthLimit);

        int[] lastScore = {0};
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
                Duration.ofSeconds(30)
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

    /**
     * Returns Stockfish's top N candidate moves for a position, ranked best
     * to worst, each with its own evaluation. Needed for detecting "only
     * move" / "great move" situations, which single-PV evaluate() cannot
     * tell you — it only ever sees the single best line.
     */
    public List<CandidateMove> evaluateTopMoves(String fen, int depthLimit, int numLines) {
        if (!isAlive()) {
            throw new EngineException("Cannot evaluate: engine process is not running (crashed or not started).");
        }

        sendCommand("setoption name MultiPV value " + numLines);
        sendCommand("position fen " + fen);
        sendCommand("go depth " + depthLimit);

        Map<Integer, CandidateMove> byRank = new TreeMap<>();
        Pattern multiPvPattern = Pattern.compile("multipv (\\d+).*?score (cp|mate) (-?\\d+).*? pv (\\S+)");

        readUntil(
                line -> line.startsWith("bestmove"),
                line -> {
                    Matcher m = multiPvPattern.matcher(line);
                    if (m.find()) {
                        int rank = Integer.parseInt(m.group(1));
                        boolean isMate = m.group(2).equals("mate");
                        int value = Integer.parseInt(m.group(3));
                        byRank.put(rank, new CandidateMove(rank, m.group(4),
                                isMate ? null : value, isMate ? value : null));
                    }
                },
                Duration.ofSeconds(30));

        // Reset back to single-line mode immediately — this engine instance
        // is pooled and shared. Leaving MultiPV elevated would silently
        // break the next caller's evaluate() call if they forgot to reset it
        // themselves (they don't have to — evaluate() also resets defensively,
        // but resetting here too means this method never depends on that).
        sendCommand("setoption name MultiPV value 1");

        if (byRank.isEmpty()) {
            throw new EngineException("Stockfish returned no candidate moves for FEN: " + fen);
        }
        return new ArrayList<>(byRank.values());
    }

    /**
     * Returns the top N candidate LINES for a position — not just the
     * first move of each (that's evaluateTopMoves, used by Brilliant/
     * Great/Good), but the engine's full principal variation per rank, for
     * on-demand practice-position display. PV length is bounded by, but
     * not guaranteed to equal, depthLimit — the engine reports whatever it
     * actually found.
     */
    public List<EngineLine> evaluateLines(String fen, int depthLimit, int numLines) {
        if (!isAlive()) {
            throw new EngineException("Cannot evaluate: engine process is not running (crashed or not started).");
        }

        sendCommand("setoption name MultiPV value " + numLines);
        sendCommand("position fen " + fen);
        sendCommand("go depth " + depthLimit);

        Map<Integer, EngineLine> byRank = new TreeMap<>();
        Pattern multiPvLinePattern = Pattern.compile("multipv (\\d+).*?score (cp|mate) (-?\\d+).*? pv (.+)$");

        readUntil(
                line -> line.startsWith("bestmove"),
                line -> {
                    Matcher m = multiPvLinePattern.matcher(line);
                    if (m.find()) {
                        int rank = Integer.parseInt(m.group(1));
                        boolean isMate = m.group(2).equals("mate");
                        int value = Integer.parseInt(m.group(3));
                        List<String> moves = List.of(m.group(4).trim().split("\\s+"));
                        byRank.put(rank, new EngineLine(rank, isMate ? null : value, isMate ? value : null, moves));
                    }
                },
                Duration.ofSeconds(30));

        sendCommand("setoption name MultiPV value 1");

        if (byRank.isEmpty()) {
            throw new EngineException("Stockfish returned no lines for FEN: " + fen);
        }
        return new ArrayList<>(byRank.values());
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
                continue;
            }
            lineHandler.accept(line);
            if (stopCondition.test(line)) {
                return line;
            }
        }
    }

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