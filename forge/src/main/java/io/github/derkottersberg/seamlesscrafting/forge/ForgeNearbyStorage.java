package io.github.derkottersberg.seamlesscrafting.forge;

import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.StackIdentity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

final class ForgeNearbyStorage implements NearbyStorage {
    private final IItemHandler handler;
    private final BlockPos pos;

    ForgeNearbyStorage(IItemHandler handler, BlockPos pos) {
        this.handler = handler;
        this.pos = pos.immutable();
    }

    @Override
    public Object identityKey() {
        return handler;
    }

    @Override
    public BlockPos key() {
        return pos;
    }

    @Override
    public List<BlockPos> positions() {
        return List.of(pos);
    }

    @Override
    public List<SlotSnapshot> snapshot() {
        List<SlotSnapshot> result = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                result.add(new SlotSnapshot(slot, stack, stack.getCount()));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public ItemStack simulateExtractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        ItemStack simulated = handler.extractItem(sourceIndex, amount, true);
        return expected.matches(simulated) && simulated.getCount() == amount ? simulated.copy() : ItemStack.EMPTY;
    }

    @Override
    public boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.getSlots()) {
            return false;
        }
        ItemStack current = handler.getStackInSlot(sourceIndex);
        if (!expected.matches(current)
            || current.getCount() < amount
            || !handler.isItemValid(sourceIndex, current)
            || current.getCount() > Math.min(current.getMaxStackSize(), handler.getSlotLimit(sourceIndex))) {
            return false;
        }
        ItemStack simulated = handler.extractItem(sourceIndex, amount, true);
        return expected.matches(simulated) && simulated.getCount() == amount;
    }

    @Override
    public ItemStack extractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        ItemStack simulated = simulateExtractExact(sourceIndex, expected, amount);
        if (simulated.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // The simulated extraction establishes exact identity on the server
        // thread. Preserve any handler result even if a broken third-party
        // implementation changes it between simulation and commit: returning
        // that stack to the grid is safer than silently deleting it.
        return handler.extractItem(sourceIndex, amount, false);
    }

    @Override
    public int insertExact(int preferredIndex, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        int before = stack.getCount();
        insert(preferredIndex, stack);
        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
            if (slot != preferredIndex) {
                insert(slot, stack);
            }
        }
        return before - stack.getCount();
    }

    private void insert(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= handler.getSlots()) {
            return;
        }
        ItemStack remainder = handler.insertItem(slot, stack.copy(), false);
        int inserted = stack.getCount() - remainder.getCount();
        if (inserted > 0) {
            stack.shrink(inserted);
        }
    }
}
