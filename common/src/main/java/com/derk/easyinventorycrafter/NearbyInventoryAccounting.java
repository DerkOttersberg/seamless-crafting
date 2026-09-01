package com.derk.easyinventorycrafter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pure item-count accounting shared by runtime scanning and unit tests. */
public final class NearbyInventoryAccounting {
    private NearbyInventoryAccounting() {
    }

    public static <K> Map<K, Long> totalCounts(Iterable<? extends Iterable<Counted<K>>> inventories) {
        Map<K, Long> totals = new LinkedHashMap<>();
        for (Iterable<Counted<K>> inventory : inventories) {
            for (Counted<K> entry : inventory) {
                if (entry.count() > 0) {
                    totals.merge(entry.key(), entry.count(), NearbyInventoryAccounting::saturatedAdd);
                }
            }
        }
        return Map.copyOf(totals);
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0 && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    public record Counted<K>(K key, long count) {
        public Counted {
            if (key == null) {
                throw new IllegalArgumentException("key cannot be null");
            }
        }
    }
}
