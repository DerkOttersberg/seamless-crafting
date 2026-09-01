package io.github.derkottersberg.seamlesscrafting.fabric;

import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.StackIdentity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

final class FabricNearbyStorage implements NearbyStorage {
    private final Storage<ItemVariant> storage;
    private final BlockPos pos;
    private final Map<StackIdentity, Integer> identityToSourceIndex = new HashMap<>();
    private final Map<Integer, StackIdentity> sourceIndexToIdentity = new HashMap<>();
    private int nextSourceIndex;

    FabricNearbyStorage(Storage<ItemVariant> storage, BlockPos pos) {
        this.storage = storage;
        this.pos = pos.immutable();
    }

    @Override
    public Object identityKey() {
        return storage;
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
        Map<StackIdentity, Long> totals = new HashMap<>();
        for (StorageView<ItemVariant> view : storage.nonEmptyViews()) {
            if (!view.isResourceBlank() && view.getAmount() > 0) {
                StackIdentity identity = StackIdentity.of(view.getResource().toStack());
                totals.merge(identity, view.getAmount(), FabricNearbyStorage::saturatedAdd);
            }
        }

        List<StackIdentity> identities = new ArrayList<>(totals.keySet());
        identities.sort(StackIdentity::compareTo);
        List<SlotSnapshot> result = new ArrayList<>(identities.size());
        for (StackIdentity identity : identities) {
            int sourceIndex = identityToSourceIndex.computeIfAbsent(identity, ignored -> {
                int allocated = nextSourceIndex++;
                sourceIndexToIdentity.put(allocated, identity);
                return allocated;
            });
            result.add(new SlotSnapshot(sourceIndex, identity.stack(), totals.get(identity)));
        }
        return List.copyOf(result);
    }

    @Override
    public ItemStack simulateExtractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (!isMappedIdentity(sourceIndex, expected) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemVariant variant = ItemVariant.of(expected.stack());
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(variant, amount, transaction);
            if (extracted != amount) {
                return ItemStack.EMPTY;
            }
            return variant.toStack(amount);
        }
    }

    @Override
    public boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int amount) {
        if (!isMappedIdentity(sourceIndex, expected) || amount <= 0) {
            return false;
        }
        ItemVariant variant = ItemVariant.of(expected.stack());
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(variant, amount, transaction);
            return extracted == amount && storage.insert(variant, amount, transaction) == amount;
        }
    }

    @Override
    public ItemStack extractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (!isMappedIdentity(sourceIndex, expected) || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemVariant variant = ItemVariant.of(expected.stack());
        try (Transaction transaction = Transaction.openOuter()) {
            long extracted = storage.extract(variant, amount, transaction);
            if (extracted != amount) {
                return ItemStack.EMPTY;
            }
            transaction.commit();
            return variant.toStack(amount);
        }
    }

    @Override
    public int insertExact(int preferredIndex, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemVariant variant = ItemVariant.of(stack);
        try (Transaction transaction = Transaction.openOuter()) {
            long inserted = storage.insert(variant, stack.getCount(), transaction);
            if (inserted <= 0) {
                return 0;
            }
            transaction.commit();
            int count = (int) Math.min(stack.getCount(), inserted);
            stack.shrink(count);
            return count;
        }
    }

    private boolean isMappedIdentity(int sourceIndex, StackIdentity expected) {
        return expected != null && expected.equals(sourceIndexToIdentity.get(sourceIndex));
    }

    private static long saturatedAdd(long first, long second) {
        if (Long.MAX_VALUE - first < second) {
            return Long.MAX_VALUE;
        }
        return first + second;
    }
}
