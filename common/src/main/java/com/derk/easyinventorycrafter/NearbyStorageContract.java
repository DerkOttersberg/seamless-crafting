package com.derk.easyinventorycrafter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.world.item.ItemStack;

/** Conservative admission checks for external item-storage implementations. */
public final class NearbyStorageContract {
    private NearbyStorageContract() {
    }

    public static boolean isUsable(NearbyStorage storage) {
        if (storage == null) {
            return false;
        }
        try {
            if (storage.identityKey() == null) {
                return false;
            }
            List<NearbyStorage.SlotSnapshot> snapshots = storage.snapshot();
            if (snapshots == null || snapshots.isEmpty()) {
                return false;
            }
            Set<Integer> sourceIndices = new HashSet<>();
            for (NearbyStorage.SlotSnapshot snapshot : snapshots) {
                if (snapshot.sourceIndex() < 0 || !sourceIndices.add(snapshot.sourceIndex())) {
                    return false;
                }
                StackIdentity expected = StackIdentity.of(snapshot.stack());
                int probeAmount = (int) Math.min(2L, snapshot.amount());
                ItemStack simulated = storage.simulateExtractExact(snapshot.sourceIndex(), expected, probeAmount);
                if (simulated.getCount() != probeAmount || !expected.matches(simulated)) {
                    return false;
                }
                if (!storage.canRestoreExactAfterExtraction(snapshot.sourceIndex(), expected, probeAmount)) {
                    return false;
                }
            }
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
