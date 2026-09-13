package com.chesscoach.backend.integrations.chesscom;

import com.chesscoach.backend.auth.CurrentUserProvider;
import com.chesscoach.backend.game.GameIngestionService;
import com.chesscoach.backend.game.GameRepository;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

import static com.chesscoach.backend.integrations.chesscom.ChessComModels.*;

@Service
public class ChessComImportService {

    private final ChessComClient chessComClient;
    private final GameIngestionService gameIngestionService;
    private final GameRepository gameRepository;
    private final CurrentUserProvider currentUserProvider;

    public ChessComImportService(ChessComClient chessComClient,
                                  GameIngestionService gameIngestionService,
                                  GameRepository gameRepository,
                                  CurrentUserProvider currentUserProvider) {
        this.chessComClient = chessComClient;
        this.gameIngestionService = gameIngestionService;
        this.gameRepository = gameRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public ChessComImportResult importMonth(String username, String timeClass, YearMonth month) {
        Long userId = currentUserProvider.requireCurrentUserId();

        ChessComArchiveResponse response =
                chessComClient.fetchMonthlyGames(username, month.getYear(), month.getMonthValue());
        List<ChessComGame> allGames = (response != null && response.games() != null) ? response.games() : List.of();

        List<ChessComGame> matching = allGames.stream()
                .filter(g -> timeClass.equalsIgnoreCase(g.timeClass()))
                .toList();

        int imported = 0, skipped = 0;
        for (ChessComGame game : matching) {
            if (gameRepository.existsBySourceUrl(game.url())) {
                skipped++;
                continue;
            }
            gameIngestionService.ingest(
                    game.pgn(),
                    game.white().username() + " vs " + game.black().username(),
                    game.white().username(), game.black().username(),
                    deriveResult(game), userId, game.url());
            imported++;
        }

        return new ChessComImportResult(matching.size(), imported, skipped);
    }

    private String deriveResult(ChessComGame game) {
        if ("win".equalsIgnoreCase(game.white().result())) return "1-0";
        if ("win".equalsIgnoreCase(game.black().result())) return "0-1";
        return "1/2-1/2"; // any non-"win" result on both sides is some form of draw
    }
}