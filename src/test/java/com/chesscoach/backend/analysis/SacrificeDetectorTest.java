package com.chesscoach.backend.analysis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SacrificeDetectorTest {

    @Test
    void detectsARealSacrifice() {
        String before = "4k3/8/8/8/8/8/8/4KQ2 w - - 0 1";  // White ahead by a queen (+9)
        String afterReply = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"; // White's queen is gone (0)

        int margin = SacrificeDetector.sacrificeMargin(before, afterReply, true);
        assertEquals(9, margin, "White giving up their whole material lead should register as a 9-point sacrifice");
    }

    @Test
    void evenTradeIsNotASacrifice() {
        String before = "4k3/8/8/8/8/8/8/4KQ2 w - - 0 1";
        String afterReply = "4k3/8/8/8/8/8/8/4KQ2 w - - 0 1"; // unchanged

        int margin = SacrificeDetector.sacrificeMargin(before, afterReply, true);
        assertEquals(0, margin, "No material change should not register as a sacrifice");
    }

    @Test
    void noReplyMeansNoSacrificeCanBeConfirmed() {
        String before = "4k3/8/8/8/8/8/8/4KQ2 w - - 0 1";
        int margin = SacrificeDetector.sacrificeMargin(before, null, true);
        assertEquals(0, margin, "A final move with no opponent reply can't be confirmed as a sacrifice");
    }

    @Test
    void worksCorrectlyFromBlacksPerspectiveToo() {
        String before = "4k2r/8/8/8/8/8/8/4K3 w - - 0 1";  // Black ahead by a rook (-5 for White = +5 for Black)
        String afterReply = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"; // Black's rook is gone

        int margin = SacrificeDetector.sacrificeMargin(before, afterReply, false);
        assertEquals(5, margin, "Black giving up their rook advantage should register as a 5-point sacrifice");
    }
}