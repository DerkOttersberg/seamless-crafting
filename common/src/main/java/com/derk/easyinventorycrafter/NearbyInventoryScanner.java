package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public final class NearbyInventoryScanner {
    public static final int DEFAULT_RADIUS = 16;
    public static final int MAX_ENTRIES = 512;
    public static final long MAX_REPORTED_COUNT = 1L << 50;
    private static final Comparator<BlockPos> POSITION_ORDER = Comparator
        .comparingInt((BlockPos pos) -> pos.getX())
        .thenComparingInt(BlockPos::getY)
        .thenComparingInt(BlockPos::getZ);

    private NearbyInventoryScanner() {
    }

    public static int getConfiguredRadius() {
        return EasyInventoryCrafterConfig.getNearbyRadius();
    }

    /**
     * Resolves the same logical inventory vanilla exposes to a player. Each
     * double chest is returned once as one 54-slot container, blocked chests
     * are excluded, locked containers are respected, and unloaded chunks are
     * never pulled in by a request.
     */
    public static List<NearbyInventory> scan(Level level, BlockPos center, int radius, Player player) {
        int boundedRadius = Math.max(1, Math.min(64, radius));
        Set<BlockPos> seenKeys = new HashSet<>();
        List<NearbyInventory> inventories = new ArrayList<>();
        BlockPos min = center.offset(-boundedRadius, -boundedRadius, -boundedRadius);
        BlockPos max = center.offset(boundedRadius, boundedRadius, boundedRadius);

        for (BlockPos mutablePos : BlockPos.betweenClosed(min, max)) {
            BlockPos pos = mutablePos.immutable();
            if (!level.isLoaded(pos)) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Container rawContainer) || rawContainer instanceof Inventory) {
                if (blockEntity == null || !seenKeys.add(pos)) {
                    continue;
                }
                NearbyStorage platformStorage = SeamlessCraftingMod.platform()
                    .findNearbyStorage(level, pos, state, blockEntity);
                if (platformStorage != null) {
                    inventories.add(new NearbyInventory(platformStorage, pos, List.of(pos)));
                }
                continue;
            }

            BlockPos connectedPos = connectedChestPosition(level, pos, state);
            BlockPos key = canonicalInventoryKey(pos, connectedPos);
            if (!seenKeys.add(key)) {
                continue;
            }

            if (!canOpen(blockEntity, player)) {
                continue;
            }
            if (connectedPos != null) {
                BlockEntity connectedEntity = level.getBlockEntity(connectedPos);
                if (connectedEntity == null || !canOpen(connectedEntity, player)) {
                    continue;
                }
            }

            Container resolved = rawContainer;
            List<BlockPos> positions = List.of(pos);
            if (state.getBlock() instanceof ChestBlock chest) {
                resolved = ChestBlock.getContainer(chest, state, level, pos, false);
                if (resolved == null) {
                    continue;
                }
                if (connectedPos != null) {
                    positions = List.of(key, key.equals(pos) ? connectedPos : pos);
                }
            }

            unpackLoot(blockEntity, player);
            if (connectedPos != null) {
                unpackLoot(level.getBlockEntity(connectedPos), player);
            }
            inventories.add(new NearbyInventory(new ContainerNearbyStorage(resolved, key, positions), key, positions));
        }

        return deduplicateStorages(inventories);
    }

    /**
     * Rejects storages that cannot prove exact, reversible extraction and
     * collapses repeated views of the same backing handler by object identity.
     */
    public static List<NearbyInventory> deduplicateStorages(List<NearbyInventory> candidates) {
        IdentityHashMap<Object, Integer> backingIndices = new IdentityHashMap<>();
        List<NearbyInventory> accepted = new ArrayList<>();
        for (NearbyInventory candidate : candidates) {
            if (candidate == null || !NearbyStorageContract.isUsable(candidate.storage())) {
                continue;
            }

            Object backing;
            try {
                backing = candidate.storage().identityKey();
            } catch (RuntimeException ignored) {
                continue;
            }
            Integer existingIndex = backingIndices.get(backing);
            if (existingIndex == null) {
                backingIndices.put(backing, accepted.size());
                accepted.add(candidate);
                continue;
            }

            NearbyInventory existing = accepted.get(existingIndex);
            LinkedHashSet<BlockPos> mergedPositions = new LinkedHashSet<>(existing.positions());
            mergedPositions.addAll(candidate.positions());
            accepted.set(existingIndex, new NearbyInventory(
                existing.storage(),
                existing.key(),
                List.copyOf(mergedPositions)
            ));
        }
        return List.copyOf(accepted);
    }

    public static List<NearbyStorage> findNearbyStorages(Level level, BlockPos center, int radius, Player player) {
        return scan(level, center, radius, player).stream().map(NearbyInventory::storage).toList();
    }

    public static List<NearbyItemEntry> collectItemCounts(List<NearbyStorage> inventories) {
        return collectNearbyItems(inventories).entries();
    }

    public static NearbyItemsSnapshot collectNearbyItems(List<NearbyStorage> inventories) {
        List<List<NearbyInventoryAccounting.Counted<StackIdentity>>> logicalInventories = new ArrayList<>();
        for (NearbyStorage inventory : inventories) {
            List<NearbyInventoryAccounting.Counted<StackIdentity>> contents = new ArrayList<>();
            for (NearbyStorage.SlotSnapshot snapshot : inventory.snapshot()) {
                contents.add(new NearbyInventoryAccounting.Counted<>(StackIdentity.of(snapshot.stack()), snapshot.amount()));
            }
            logicalInventories.add(contents);
        }

        List<Map.Entry<StackIdentity, Long>> totals = NearbyInventoryAccounting.totalCounts(logicalInventories)
            .entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
        boolean truncated = totals.size() > MAX_ENTRIES;
        List<NearbyItemEntry> entries = new ArrayList<>();
        for (int index = 0; index < Math.min(totals.size(), MAX_ENTRIES); index++) {
            Map.Entry<StackIdentity, Long> entry = totals.get(index);
            long boundedCount = Math.min(entry.getValue(), MAX_REPORTED_COUNT);
            truncated |= boundedCount != entry.getValue();
            entries.add(new NearbyItemEntry(entry.getKey().stack(), boundedCount));
        }

        RecipeFinderResult recipeFinder = collectRecipeFinderResult(inventories);
        return new NearbyItemsSnapshot(entries, recipeFinder.stacks(), truncated || recipeFinder.truncated());
    }

    public static List<ItemStack> collectRecipeFinderStacks(List<NearbyStorage> inventories) {
        return collectRecipeFinderResult(inventories).stacks();
    }

    private static RecipeFinderResult collectRecipeFinderResult(List<NearbyStorage> inventories) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int storageIndex = 0; storageIndex < inventories.size(); storageIndex++) {
            List<NearbyStorage.SlotSnapshot> snapshots = inventories.get(storageIndex).snapshot();
            for (int snapshotIndex = 0; snapshotIndex < snapshots.size(); snapshotIndex++) {
                NearbyStorage.SlotSnapshot snapshot = snapshots.get(snapshotIndex);
                long remaining = Math.min(snapshot.amount(), (long) snapshot.stack().getMaxStackSize() * 9L);
                while (remaining > 0 && stacks.size() < MAX_ENTRIES) {
                    int count = (int) Math.min(snapshot.stack().getMaxStackSize(), remaining);
                    stacks.add(snapshot.stack().copyWithCount(count));
                    remaining -= count;
                }
                if (stacks.size() >= MAX_ENTRIES) {
                    boolean hasMore = remaining > 0
                        || snapshotIndex + 1 < snapshots.size()
                        || storageIndex + 1 < inventories.size();
                    return new RecipeFinderResult(stacks, hasMore);
                }
            }
        }
        return new RecipeFinderResult(stacks, false);
    }

    public static List<BlockPos> findInventoryPositionsWithItem(
        Level level,
        BlockPos center,
        int radius,
        Player player,
        ItemStack requested
    ) {
        List<BlockPos> positions = new ArrayList<>();
        for (NearbyInventory inventory : scan(level, center, radius, player)) {
            if (inventoryHasItem(inventory.storage(), requested)) {
                for (BlockPos pos : inventory.positions()) {
                    if (positions.size() >= MAX_ENTRIES) {
                        return List.copyOf(positions);
                    }
                    positions.add(pos);
                }
            }
        }
        return List.copyOf(positions);
    }

    @Nullable
    public static BlockPos findFirstInventoryPosWithItem(
        Level level,
        BlockPos center,
        int radius,
        Player player,
        ItemStack requested
    ) {
        List<BlockPos> positions = findInventoryPositionsWithItem(level, center, radius, player, requested);
        return positions.isEmpty() ? null : positions.getFirst();
    }

    public static BlockPos canonicalInventoryKey(BlockPos first, @Nullable BlockPos second) {
        if (second == null || POSITION_ORDER.compare(first, second) <= 0) {
            return first.immutable();
        }
        return second.immutable();
    }

    @Nullable
    private static BlockPos connectedChestPosition(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof ChestBlock) || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) {
            return null;
        }
        BlockPos connected = ChestBlock.getConnectedBlockPos(pos, state);
        if (!level.isLoaded(connected) || !(level.getBlockState(connected).getBlock() instanceof ChestBlock)) {
            return null;
        }
        return connected.immutable();
    }

    private static boolean canOpen(BlockEntity blockEntity, Player player) {
        return !(blockEntity instanceof BaseContainerBlockEntity container) || container.canOpen(player);
    }

    private static void unpackLoot(@Nullable BlockEntity blockEntity, Player player) {
        if (blockEntity instanceof RandomizableContainer randomizable) {
            randomizable.unpackLootTable(player);
        }
    }

    private static boolean inventoryHasItem(NearbyStorage inventory, ItemStack requested) {
        StackIdentity identity = StackIdentity.of(requested);
        for (NearbyStorage.SlotSnapshot snapshot : inventory.snapshot()) {
            if (identity.matches(snapshot.stack())) {
                return true;
            }
        }
        return false;
    }

    public record NearbyInventory(NearbyStorage storage, BlockPos key, List<BlockPos> positions) {
        public NearbyInventory {
            positions = List.copyOf(positions);
        }

        public Container container() {
            if (storage instanceof ContainerNearbyStorage containerStorage) {
                return containerStorage.container();
            }
            throw new IllegalStateException("Nearby storage is not a vanilla Container");
        }
    }

    public record NearbyItemEntry(ItemStack stack, long count) {
        public NearbyItemEntry {
            if (stack == null || stack.isEmpty()) {
                throw new IllegalArgumentException("entry stack must not be empty");
            }
            if (count <= 0) {
                throw new IllegalArgumentException("entry count must be positive");
            }
            stack = stack.copyWithCount(1);
        }
    }

    public record NearbyItemsSnapshot(
        List<NearbyItemEntry> entries,
        List<ItemStack> recipeFinderStacks,
        boolean truncated
    ) {
        public NearbyItemsSnapshot {
            entries = List.copyOf(entries);
            recipeFinderStacks = recipeFinderStacks.stream().map(ItemStack::copy).toList();
        }
    }

    private record RecipeFinderResult(List<ItemStack> stacks, boolean truncated) {
        private RecipeFinderResult {
            stacks = stacks.stream().map(ItemStack::copy).toList();
        }
    }

    public record WorldPos(Level level, BlockPos pos) {
    }
}
