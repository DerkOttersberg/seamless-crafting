package io.github.derkottersberg.seamlesscrafting.neoforge;

import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.StackIdentity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

final class NeoForgeNearbyStorage implements NearbyStorage {
    private final ResourceHandler<ItemResource> handler;
    private final BlockPos pos;

    NeoForgeNearbyStorage(ResourceHandler<ItemResource> handler, BlockPos pos) {
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
        for (int slot = 0; slot < handler.size(); slot++) {
            ItemResource resource = handler.getResource(slot);
            long amount = handler.getAmountAsLong(slot);
            if (!resource.isEmpty() && amount > 0) {
                result.add(new SlotSnapshot(slot, resource.toStack(), amount));
            }
        }
        return List.copyOf(result);
    }

    @Override
    public ItemStack simulateExtractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.size()) {
            return ItemStack.EMPTY;
        }
        ItemResource resource = ItemResource.of(expected.stack());
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(sourceIndex, resource, amount, transaction);
            return extracted == amount ? resource.toStack(amount) : ItemStack.EMPTY;
        }
    }

    @Override
    public boolean canRestoreExactAfterExtraction(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.size()) {
            return false;
        }
        ItemResource resource = ItemResource.of(expected.stack());
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(sourceIndex, resource, amount, transaction);
            if (extracted != amount) {
                return false;
            }
            int restored = handler.insert(sourceIndex, resource, amount, transaction);
            if (restored < amount) {
                restored += handler.insert(resource, amount - restored, transaction);
            }
            return restored == amount;
        }
    }

    @Override
    public ItemStack extractExact(int sourceIndex, StackIdentity expected, int amount) {
        if (amount <= 0 || sourceIndex < 0 || sourceIndex >= handler.size()) {
            return ItemStack.EMPTY;
        }
        ItemResource resource = ItemResource.of(expected.stack());
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(sourceIndex, resource, amount, transaction);
            if (extracted != amount) {
                return ItemStack.EMPTY;
            }
            transaction.commit();
            return resource.toStack(extracted);
        }
    }

    @Override
    public int insertExact(int preferredIndex, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        ItemResource resource = ItemResource.of(stack);
        try (Transaction transaction = Transaction.openRoot()) {
            int requested = stack.getCount();
            int inserted = 0;
            if (preferredIndex >= 0 && preferredIndex < handler.size()) {
                inserted = handler.insert(preferredIndex, resource, requested, transaction);
            }
            if (inserted < requested) {
                inserted += handler.insert(resource, requested - inserted, transaction);
            }
            if (inserted <= 0) {
                return 0;
            }
            transaction.commit();
            stack.shrink(inserted);
            return inserted;
        }
    }
}
