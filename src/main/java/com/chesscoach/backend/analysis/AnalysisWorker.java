package com.chesscoach.backend.analysis;

import com.chesscoach.backend.analysis.engine.EngineEvaluation;
import com.chesscoach.backend.analysis.engine.StockfishEngine;
import com.chesscoach.backend.analysis.engine.StockfishEnginePool;
import com.chesscoach.backend.game.Move;
import com.chesscoach.backend.game.MoveRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Consumer side of the analysis queue: runs on its own dedicated thread,
 * blocking-pops jobs from Redis, and analyzes every move in the game
 * using an engine borrowed from the pool.
 *
 * ANALYSIS DEPTH: fixed at 12 for now — deliberately shallow. This is the
 * "fast first pass" half of the two-pass strategy from the roadmap; a
 * second, deeper re-analysis pass targeting only the biggest eval swings
 * is future work (Phase 4), not part of this file.
 */
@Component
public class AnalysisWorker {

    private static final Logger log = LoggerFactory.getLogger(AnalysisWorker.class);
    private static final int SEARCH_DEPTH = 12;
    private static final Duration BORROW_TIMEOUT = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final StockfishEnginePool enginePool;
    private final MoveRepository moveRepository;
    private final MoveEvaluationRepository moveEvaluationRepository;

    private ExecutorService workerThread;
    private volatile boolean running = true;

    public AnalysisWorker(StringRedisTemplate redisTemplate,
                           ObjectMapper objectMapper,
                           StockfishEnginePool enginePool,
                           MoveRepository moveRepository,
                           MoveEvaluationRepository moveEvaluationRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enginePool = enginePool;
        this.moveRepository = moveRepository;
        this.moveEvaluationRepository = moveEvaluationRepository;
    }

    @PostConstruct
    public void start() {
        // A single dedicated background thread, not a request-handling thread.
        // This loop runs for the app's entire lifetime, independent of any
        // HTTP request — that independence is the whole point of Phase 3:
        // uploads return instantly, analysis happens on its own schedule.
        workerThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "analysis-worker");
            t.setDaemon(true);
            return t;
        });
        workerThread.submit(this::runLoop);
        log.info("AnalysisWorker started");
    }

    private void runLoop() {
        while (running) {
            try {
                // BRPOP blocks server-side in Redis for up to 5s waiting for a job,
                // rather than us polling in a tight loop wasting CPU. Short timeout
                // (not indefinite) so the loop still checks `running` periodically
                // and can shut down promptly.
                String payload = redisTemplate.opsForList()
                        .rightPop(AnalysisQueueService.QUEUE_KEY, Duration.ofSeconds(5));

                if (payload == null) {
                    continue; // nothing queued this cycle, loop and check again
                }

                AnalysisJob job = objectMapper.readValue(payload, AnalysisJob.class);
                processJob(job);

            } catch (Exception e) {
                // A single bad job must never kill the whole worker loop —
                // log it and keep processing the queue. This is the difference
                // between "one game fails to analyze" and "analysis is dead
                // for every user until someone notices and restarts the app."
                log.error("Error processing analysis job", e);
            }
        }
    }

    private void processJob(AnalysisJob job) {
        log.info("Analyzing game {}", job.gameId());
        List<Move> moves = moveRepository.findByGameIdOrderByPlyNumberAsc(job.gameId());

        if (moves.isEmpty()) {
            log.warn("No moves found for game {}, skipping analysis", job.gameId());
            return;
        }

        StockfishEngine engine = enginePool.borrow(BORROW_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        try {
            for (Move move : moves) {
                analyzeAndStore(engine, move);
            }
            log.info("Finished analyzing game {} ({} moves)", job.gameId(), moves.size());
        } finally {
            // Always release, even if one move's evaluation throws partway through —
            // otherwise a single failure permanently shrinks the pool by one engine.
            enginePool.release(engine);
        }
    }

    @Transactional
    protected void analyzeAndStore(StockfishEngine engine, Move move) {
        EngineEvaluation eval = engine.evaluate(move.getFenAfter(), SEARCH_DEPTH);
        MoveEvaluation entity = new MoveEvaluation(
                move, eval.bestMoveUci(), eval.scoreCentipawns(), eval.mateInMoves());
        moveEvaluationRepository.save(entity);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (workerThread != null) {
            workerThread.shutdown();
        }
        log.info("AnalysisWorker stopped");
    }
}