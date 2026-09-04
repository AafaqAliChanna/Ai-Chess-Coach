package com.chesscoach.backend.analysis.engine;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the "stockfish.*" keys from application.yml / application-local.yml.
 * poolSize is nullable on purpose — null means "auto-detect from CPU cores"
 * (see StockfishEnginePool.resolvePoolSize). Only override it explicitly
 * if you need to cap engine count below what your hardware would give you.
 */
@Component
@ConfigurationProperties(prefix = "stockfish")
public class StockfishProperties {

    private String path;
    private Integer poolSize;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(Integer poolSize) {
        this.poolSize = poolSize;
    }
}