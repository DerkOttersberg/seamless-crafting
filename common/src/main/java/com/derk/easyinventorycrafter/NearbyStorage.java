package com.derk.easyinventorycrafter;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral view of a nearby item storage. All methods run on the server thread. */
public interface NearbyStorage {
    /** Stable backing-object identity used to collapse multi-position capability views. */
    default Object identityKey() {
        return this;
    }

    BlockPos key();

    List<BlockPos> positions();

    List<SlotSnapshot> snapshot();

    /** Non-mutating exact extraction probe used before a handler is admitted. */
    default ItemStack simulateExtractExact(int sourceIndex, StackIdentity expected, int amount) {
        return ItemStack.EMPTY;
    }

    /**
     * Returns whether an extraction of {@code amount} can be restored exactly.
     * Implementations must not commit any mutation while answering.
     */
    default boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int amount) {
        return false;
    }

    /** Extracts up to {@code amount}, but only from the exact expected component identity. */
    ItemStack extractExact(int sourceIndex, StackIdentity expected, int amount);

    /** Inserts as much as possible and returns the inserted item count. */
    int insertExact(int preferredIndex, ItemStack stack);

    default void markChanged() {
    }

    record SlotSnapshot(int sourceIndex, ItemStack stack, long amount) {
        public SlotSnapshot {
            if (stack == null || stack.isEmpty()) {
                throw new IllegalArgumentException("snapshot stack must not be empty");
            }
            if (amount <= 0) {
                throw new IllegalArgumentException("snapshot amount must be positive");
            }
            stack = stack.copyWithCount(1);
        }
    }
}
