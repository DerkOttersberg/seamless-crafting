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
        if (expected == null || amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }
        HandlerSnapshot before = captureSnapshot();
        ItemStack first = handler.extractItem(sourceIndex, amount, true);
        if (!isExact(first, expected, amount) || !before.matches(captureSnapshot())) {
            return ItemStack.EMPTY;
        }
        ItemStack second = handler.extractItem(sourceIndex, amount, true);
        if (!isExact(second, expected, amount)
            || !ItemStack.isSameItemSameComponents(first, second)
            || !before.matches(captureSnapshot())) {
            return ItemStack.EMPTY;
        }
        return first.copy();
    }

    @Override
    public boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int amount) {
        if (expected == null || amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.getSlots()) {
            return false;
        }
        HandlerSnapshot before = captureSnapshot();
        ItemStack current = before.stack(sourceIndex);
        if (!expected.matches(current)
            || current.getCount() < amount
            || !handler.isItemValid(sourceIndex, current)
            || !handler.isItemValid(sourceIndex, current.copy())
            || current.getCount() > Math.min(current.getMaxStackSize(), before.slotLimit(sourceIndex))) {
            return false;
        }

        ItemStack simulated = simulateExtractExact(sourceIndex, expected, amount);
        if (!isExact(simulated, expected, amount) || !before.matches(captureSnapshot())) {
            return false;
        }

        /*
         * Forge item handlers do not expose a transaction that can chain a
         * simulated extraction and insertion. Therefore, only admit a handler
         * when it can already accept the complete rollback stack without
         * relying on the space that extraction might free. This is deliberately
         * conservative, but it is an actual simulated restoration proof rather
         * than an inference from isItemValid/getSlotLimit.
         *
         * Run the proof twice. Handlers whose simulated insertion is stateful,
         * or whose simulation mutates their visible contents, are unsafe for a
         * transaction rollback and must not enter recipe planning.
         */
        InsertionSimulation firstRestoration = simulateCompleteInsertion(simulated);
        if (firstRestoration == null || !before.matches(captureSnapshot())) {
            return false;
        }
        InsertionSimulation secondRestoration = simulateCompleteInsertion(simulated);
        return firstRestoration.equals(secondRestoration) && before.matches(captureSnapshot());
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

    private InsertionSimulation simulateCompleteInsertion(ItemStack stack) {
        int remaining = stack.getCount();
        List<Integer> acceptedBySlot = new ArrayList<>(handler.getSlots());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (remaining <= 0) {
                acceptedBySlot.add(0);
                continue;
            }
            ItemStack offered = stack.copyWithCount(remaining);
            ItemStack input = offered.copy();
            ItemStack remainder = handler.insertItem(slot, input, true);
            if (!isUnchanged(input, offered) || !isValidRemainder(remainder, offered)) {
                return null;
            }
            int rejected = remainder.isEmpty() ? 0 : remainder.getCount();
            acceptedBySlot.add(remaining - rejected);
            remaining = rejected;
        }
        return remaining == 0 ? new InsertionSimulation(acceptedBySlot) : null;
    }

    private HandlerSnapshot captureSnapshot() {
        int slots = handler.getSlots();
        if (slots < 0) {
            throw new IllegalStateException("Forge item handler reported a negative slot count");
        }
        List<ItemStack> stacks = new ArrayList<>(slots);
        List<Integer> slotLimits = new ArrayList<>(slots);
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack == null) {
                throw new IllegalStateException("Forge item handler returned a null stack");
            }
            stacks.add(stack.copy());
            slotLimits.add(handler.getSlotLimit(slot));
        }
        return new HandlerSnapshot(stacks, slotLimits);
    }

    private static boolean isExact(ItemStack stack, StackIdentity expected, int amount) {
        return stack != null && expected.matches(stack) && stack.getCount() == amount;
    }

    private static boolean isUnchanged(ItemStack actual, ItemStack expected) {
        return actual.getCount() == expected.getCount()
            && (actual.isEmpty() && expected.isEmpty() || ItemStack.isSameItemSameComponents(actual, expected));
    }

    private static boolean isValidRemainder(ItemStack remainder, ItemStack offered) {
        if (remainder == null) {
            return false;
        }
        return remainder.isEmpty()
            || ItemStack.isSameItemSameComponents(remainder, offered)
            && remainder.getCount() > 0
            && remainder.getCount() <= offered.getCount();
    }

    private record HandlerSnapshot(List<ItemStack> stacks, List<Integer> slotLimits) {
        private HandlerSnapshot {
            stacks = stacks.stream().map(ItemStack::copy).toList();
            slotLimits = List.copyOf(slotLimits);
        }

        private ItemStack stack(int slot) {
            return stacks.get(slot).copy();
        }

        private int slotLimit(int slot) {
            return slotLimits.get(slot);
        }

        private boolean matches(HandlerSnapshot other) {
            if (other == null || stacks.size() != other.stacks.size() || !slotLimits.equals(other.slotLimits)) {
                return false;
            }
            for (int slot = 0; slot < stacks.size(); slot++) {
                if (!isUnchanged(stacks.get(slot), other.stacks.get(slot))) {
                    return false;
                }
            }
            return true;
        }
    }

    private record InsertionSimulation(List<Integer> acceptedBySlot) {
        private InsertionSimulation {
            acceptedBySlot = List.copyOf(acceptedBySlot);
        }
    }
}
