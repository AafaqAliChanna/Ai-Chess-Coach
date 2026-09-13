package com.chesscoach.backend.integrations.chesscom;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static com.chesscoach.backend.integrations.chesscom.ChessComModels.ChessComArchiveResponse;

@Component
public class ChessComClient {

    private final RestClient restClient;

    public ChessComClient() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.chess.com")
                // REQUIRED by Chess.com — requests with no descriptive
                // User-Agent get a 403. Replace the email below with a
                // real one you control.
                .defaultHeader("User-Agent", "AIChessCoach/1.0 (contact: aaafaq876@gmail.com)")
                .build();
    }

    public ChessComArchiveResponse fetchMonthlyGames(String username, int year, int month) {
        String monthPadded = String.format("%02d", month);
        try {
            return restClient.get()
                    .uri("/pub/player/{username}/games/{year}/{month}", username, year, monthPadded)
                    .retrieve()
                    .body(ChessComArchiveResponse.class);
        } catch (RestClientException e) {
            throw new ChessComImportException(
                    "Failed to fetch Chess.com games for \"" + username + "\" (" + year + "-" + monthPadded
                            + "). Check the username is correct and has games in that month.", e);
        }
    }
}