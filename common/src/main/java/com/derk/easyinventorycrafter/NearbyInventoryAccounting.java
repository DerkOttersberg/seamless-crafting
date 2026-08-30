package com.derk.easyinventorycrafter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pure item-count accounting shared by runtime scanning and unit tests. */
public final class NearbyInventoryAccounting {
    private NearbyInventoryAccounting() {
    }

    public static <K> Map<K, Integer> totalCounts(Iterable<? extends Iterable<Counted<K>>> inventories) {
        Map<K, Integer> totals = new LinkedHashMap<>();
        for (Iterable<Counted<K>> inventory : inventories) {
            for (Counted<K> entry : inventory) {
                if (entry.count() > 0) {
                    totals.merge(entry.key(), entry.count(), NearbyInventoryAccounting::saturatedAdd);
                }
            }
        }
        return Map.copyOf(totals);
    }

    private static int saturatedAdd(int left, int right) {
        long sum = (long) left + right;
        return sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    public record Counted<K>(K key, int count) {
        public Counted {
            if (key == null) {
                throw new IllegalArgumentException("key cannot be null");
            }
        }
    }
}
