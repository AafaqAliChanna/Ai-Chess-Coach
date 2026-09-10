package com.chesscoach.backend.coach;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static com.chesscoach.backend.coach.GeminiModels.*;

@Component
@ConditionalOnProperty(name = "coaching.provider", havingValue = "gemini")
public class GeminiClient implements LlmClient {

    private final RestClient restClient;
    private final GeminiProperties properties;

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public String generate(String prompt) {
        try {
            GenerateResponse response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                    .header("x-goog-api-key", properties.getApiKey())
                    .body(GenerateRequest.ofPrompt(prompt))
                    .retrieve()
                    .body(GenerateResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new CoachingException("Gemini returned no candidates — the prompt may have been blocked by safety filters.");
            }

            return response.candidates().get(0).content().parts().get(0).text();

        } catch (RestClientException e) {
            // Most likely causes: missing/invalid API key, or free-tier rate
            // limit hit (Gemini returns 429 RESOURCE_EXHAUSTED when exceeded).
            throw new CoachingException(
                    "Failed to reach Gemini API — check gemini.api-key is set correctly, "
                            + "or you may have hit the free-tier rate limit (retry in a minute).", e);
        }
    }
}