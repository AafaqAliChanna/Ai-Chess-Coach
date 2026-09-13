package com.chesscoach.backend.integrations.chesscom;

import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/games/import/chesscom")
public class ChessComImportController {

    private final ChessComImportService chessComImportService;

    public ChessComImportController(ChessComImportService chessComImportService) {
        this.chessComImportService = chessComImportService;
    }

    @PostMapping
    public ChessComImportResult importGames(
            @RequestParam String username,
            @RequestParam(defaultValue = "rapid") String timeClass,
            @RequestParam(required = false) String month) {
        YearMonth targetMonth = (month != null) ? YearMonth.parse(month) : YearMonth.now().minusMonths(1);
        return chessComImportService.importMonth(username, timeClass, targetMonth);
    }
}