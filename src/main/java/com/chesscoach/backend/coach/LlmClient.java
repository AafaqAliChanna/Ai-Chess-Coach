package com.chesscoach.backend.coach;

/**
 * Common contract for any LLM provider we can plug into the coaching layer.
 * Implemented by both OllamaClient (local, free, slow) and GeminiClient
 * (hosted, free tier, fast). Which one is active is a config decision
 * (coaching.provider), not a code decision — CoachingService never knows
 * or cares which implementation it's actually talking to.
 */
public interface LlmClient {
    String generate(String prompt);
}