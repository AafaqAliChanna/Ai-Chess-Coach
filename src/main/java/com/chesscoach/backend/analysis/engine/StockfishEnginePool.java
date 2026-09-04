package com.chesscoach.backend.analysis.engine;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages a fixed set of StockfishEngine instances sized to available hardware.
 * Callers borrow() an engine, use it exclusively, and MUST release() it back —
 * this class does not enforce that at compile time, so every call site
 * (AnalysisWorker, next step) wraps usage in try/finally.
 */
@Component
public class StockfishEnginePool {

    private static final Logger log = LoggerFactory.getLogger(StockfishEnginePool.class);

    private final StockfishProperties properties;
    private final BlockingQueue<StockfishEngine> available = new LinkedBlockingQueue<>();
    private final List<StockfishEngine> allEngines = new ArrayList<>();
    private int poolSize;

    public StockfishEnginePool(StockfishProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void initialize() {
        poolSize = resolvePoolSize();
        log.info("Starting Stockfish engine pool with {} instance(s), binary: {}", poolSize, properties.getPath());

        for (int i = 0; i < poolSize; i++) {
            StockfishEngine engine = startNewEngine();
            allEngines.add(engine);
            available.add(engine);
        }
    }

    private int resolvePoolSize() {
        if (properties.getPoolSize() != null && properties.getPoolSize() > 0) {
            return properties.getPoolSize();
        }
        // Leave one core free for the JVM/web server/OS itself — pegging every
        // core to engine processes would starve the rest of the application
        // under load, exactly the kind of self-inflicted bottleneck we want
        // to avoid rather than discover in production.
        int cores = Runtime.getRuntime().availableProcessors();
        return Math.max(1, cores - 1);
    }

    private StockfishEngine startNewEngine() {
        StockfishEngine engine = new StockfishEngine(properties.getPath());
        engine.start();
        return engine;
    }

    public StockfishEngine borrow(long timeout, TimeUnit unit) {
        try {
            StockfishEngine engine = available.poll(timeout, unit);
            if (engine == null) {
                throw new EngineException(
                        "Timed out after " + timeout + " " + unit + " waiting for an available Stockfish engine "
                                + "(pool size: " + poolSize + ", all currently busy).");
            }
            return engine;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EngineException("Interrupted while waiting for an available engine", e);
        }
    }

    /**
     * Returns an engine after use. If it died while borrowed (crashed mid-analysis),
     * it's replaced with a fresh instance rather than handed back — returning a dead
     * engine to the pool would just push the same crash onto the next caller.
     */
    public void release(StockfishEngine engine) {
        if (engine.isAlive()) {
            available.add(engine);
        } else {
            log.warn("Stockfish engine died while in use — starting a replacement instance");
            allEngines.remove(engine);
            StockfishEngine replacement = startNewEngine();
            allEngines.add(replacement);
            available.add(replacement);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down {} Stockfish engine(s)", allEngines.size());
        for (StockfishEngine engine : allEngines) {
            engine.close();
        }
    }
}