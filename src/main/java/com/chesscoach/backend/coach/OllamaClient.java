package com.chesscoach.backend.coach;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "coaching.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OllamaClient.class);

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(OllamaProperties properties) {
        this.properties = properties;
        // A dedicated RestClient per this class, not a shared/global bean —
        // keeps Ollama-specific config (base URL) local to the one place
        // that talks to Ollama, rather than a generic HTTP bean other
        // unrelated services might accidentally start depending on.
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    /**
     * stream:false means Ollama returns one complete JSON response instead
     * of a stream of partial tokens. Simpler to consume for now — real-time
     * streaming to the frontend is a nice-to-have UX improvement, not a V1
     * requirement, and adds real complexity (SSE/websockets) we don't need yet.
     */
    @Override
    public String generate(String prompt) {
        try {
            OllamaGenerateResponse response = restClient.post()
                    .uri("/api/generate")
                    .body(new OllamaGenerateRequest(properties.getModel(), prompt, false))
                    .retrieve()
                    .body(OllamaGenerateResponse.class);

            if (response == null || response.response() == null) {
                throw new CoachingException("Ollama returned an empty response");
            }
            return response.response();

        } catch (RestClientException e) {
            // Most likely causes right now: Ollama not running, or the model
            // isn't pulled yet (still downloading, in your case today).
            throw new CoachingException(
                    "Failed to reach Ollama at " + properties.getBaseUrl()
                            + " — is it running, and is model \"" + properties.getModel() + "\" pulled?", e);
        }
    }
}