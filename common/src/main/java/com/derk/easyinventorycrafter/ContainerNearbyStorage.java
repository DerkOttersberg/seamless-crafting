package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Component-preserving adapter for vanilla and modded {@link Container} implementations. */
public final class ContainerNearbyStorage implements NearbyStorage {
    private final Container container;
    private final BlockPos key;
    private final List<BlockPos> positions;

    public ContainerNearbyStorage(Container container, BlockPos key, List<BlockPos> positions) {
        this.container = container;
        this.key = key.immutable();
        this.positions = List.copyOf(positions);
    }

    public Container container() {
        return container;
    }

    @Override
    public Object identityKey() {
        return container;
    }

    @Override
    public BlockPos key() {
        return key;
    }

    @Override
    public List<BlockPos> positions() {
        return positions;
    }

    @Override
    public List<SlotSnapshot> snapshot() {
        List<SlotSnapshot> snapshots = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty()) {
                snapshots.add(new SlotSnapshot(slot, stack, stack.getCount()));
            }
        }
        return List.copyOf(snapshots);
    }

    @Override
    public ItemStack simulateExtractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= container.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = container.getItem(sourceIndex);
        if (!expected.matches(current) || current.getCount() < amount) {
            return ItemStack.EMPTY;
        }
        return current.copyWithCount(amount);
    }

    @Override
    public boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= container.getContainerSize()) {
            return false;
        }
        ItemStack current = container.getItem(sourceIndex);
        return expected.matches(current)
            && current.getCount() >= amount
            && container.canPlaceItem(sourceIndex, current)
            && current.getCount() <= Math.min(current.getMaxStackSize(), container.getMaxStackSize(current));
    }

    @Override
    public ItemStack extractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= container.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        ItemStack current = container.getItem(sourceIndex);
        if (!expected.matches(current)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = container.removeItem(sourceIndex, Math.min(amount, current.getCount()));
        if (!removed.isEmpty()) {
            container.setChanged();
        }
        return removed;
    }

    @Override
    public int insertExact(int preferredIndex, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int before = stack.getCount();
        tryInsert(preferredIndex, stack);
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            if (slot != preferredIndex) {
                tryInsert(slot, stack);
            }
        }
        int inserted = before - stack.getCount();
        if (inserted > 0) {
            container.setChanged();
        }
        return inserted;
    }

    private void tryInsert(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= container.getContainerSize() || !container.canPlaceItem(slot, stack)) {
            return;
        }
        ItemStack target = container.getItem(slot);
        if (!target.isEmpty() && !ItemStack.isSameItemSameComponents(target, stack)) {
            return;
        }
        int limit = Math.min(stack.getMaxStackSize(), container.getMaxStackSize(stack));
        if (target.isEmpty()) {
            int inserted = Math.min(limit, stack.getCount());
            if (inserted > 0) {
                container.setItem(slot, stack.copyWithCount(inserted));
                stack.shrink(inserted);
            }
        } else {
            int inserted = Math.min(stack.getCount(), limit - target.getCount());
            if (inserted > 0) {
                target.grow(inserted);
                stack.shrink(inserted);
            }
        }
    }

    @Override
    public void markChanged() {
        container.setChanged();
    }
}
