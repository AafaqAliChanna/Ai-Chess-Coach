package com.chesscoach.backend.analysis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AnalysisQueueService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisQueueService.class);

    // A plain Redis list used as a FIFO queue: producers LPUSH, the worker BRPOPs.
    // Named with a colon namespace ("analysis:queue") — standard Redis convention
    // so future keys (e.g. "analysis:results:{id}") are easy to scan/group.
    static final String QUEUE_KEY = "analysis:queue";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AnalysisQueueService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void enqueue(AnalysisJob job) {
        try {
            String payload = objectMapper.writeValueAsString(job);
            redisTemplate.opsForList().leftPush(QUEUE_KEY, payload);
            log.info("Enqueued analysis job for game {}", job.gameId());
        } catch (JsonProcessingException e) {
            // A job that fails to serialize is a real bug (not transient), and
            // failing loudly here beats silently dropping the analysis request.
            throw new IllegalStateException("Failed to serialize AnalysisJob for game " + job.gameId(), e);
        }
    }

    /**
     * Publish only after the database transaction commits. The worker can
     * otherwise consume the job while the upload transaction is still
     * uncommitted and observe zero moves, permanently losing the job.
     */
    public void enqueueAfterCommit(AnalysisJob job) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            enqueue(job);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                enqueue(job);
            }
        });
    }
}