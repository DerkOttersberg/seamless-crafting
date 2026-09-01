package com.derk.easyinventorycrafter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class NearbyInventoryAccountingTest {
    @Test
    void eitherDoubleChestHalfProducesTheSameCanonicalKey() {
        BlockPos left = new BlockPos(10, 64, -4);
        BlockPos right = new BlockPos(11, 64, -4);

        assertEquals(
            NearbyInventoryScanner.canonicalInventoryKey(left, right),
            NearbyInventoryScanner.canonicalInventoryKey(right, left)
        );
        assertEquals(left, NearbyInventoryScanner.canonicalInventoryKey(left, right));
    }

    @Test
    void bothHalvesOfOneLogicalChestAreAccountedExactlyOnce() {
        var leftHalf = List.of(new NearbyInventoryAccounting.Counted<>("oak_log", 5));
        var rightHalf = List.of(
            new NearbyInventoryAccounting.Counted<>("oak_log", 7),
            new NearbyInventoryAccounting.Counted<>("cobblestone", 9)
        );

        assertEquals(
            Map.of("oak_log", 12L, "cobblestone", 9L),
            NearbyInventoryAccounting.totalCounts(List.of(leftHalf, rightHalf))
        );
    }

    @Test
    void countsSaturateInsteadOfOverflowing() {
        var almostFull = List.of(new NearbyInventoryAccounting.Counted<>("stone", Long.MAX_VALUE - 4));
        var additional = List.of(new NearbyInventoryAccounting.Counted<>("stone", 64));

        assertEquals(
            Long.MAX_VALUE,
            NearbyInventoryAccounting.totalCounts(List.of(almostFull, additional)).get("stone")
        );
    }
}
