package com.derk.easyinventorycrafter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.jetbrains.annotations.Nullable;

public final class NearbyInventoryScanner {
    public static final int DEFAULT_RADIUS = 16;
    public static final int MAX_ENTRIES = 512;
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

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof Container rawContainer) || rawContainer instanceof Inventory) {
                continue;
            }

            BlockState state = level.getBlockState(pos);
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
            inventories.add(new NearbyInventory(resolved, key, positions));
        }

        return inventories;
    }

    public static List<Container> findNearbyInventories(Level level, BlockPos center, int radius, Player player) {
        return scan(level, center, radius, player).stream().map(NearbyInventory::container).toList();
    }

    public static List<NearbyItemEntry> collectItemCounts(List<Container> inventories) {
        List<List<NearbyInventoryAccounting.Counted<Item>>> logicalInventories = new ArrayList<>();
        for (Container inventory : inventories) {
            List<NearbyInventoryAccounting.Counted<Item>> contents = new ArrayList<>();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()) {
                    contents.add(new NearbyInventoryAccounting.Counted<>(stack.getItem(), stack.getCount()));
                }
            }
            logicalInventories.add(contents);
        }

        return NearbyInventoryAccounting.totalCounts(logicalInventories).entrySet().stream()
            .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
            .limit(MAX_ENTRIES)
            .map(entry -> new NearbyItemEntry(new ItemStack(entry.getKey()), entry.getValue()))
            .toList();
    }

    public static List<ItemStack> collectRecipeFinderStacks(List<Container> inventories) {
        List<ItemStack> stacks = new ArrayList<>();
        for (Container inventory : inventories) {
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty()) {
                    stacks.add(stack.copy());
                    if (stacks.size() >= MAX_ENTRIES) {
                        return List.copyOf(stacks);
                    }
                }
            }
        }
        return List.copyOf(stacks);
    }

    public static List<BlockPos> findInventoryPositionsWithItem(
        Level level,
        BlockPos center,
        int radius,
        Player player,
        Item item
    ) {
        List<BlockPos> positions = new ArrayList<>();
        for (NearbyInventory inventory : scan(level, center, radius, player)) {
            if (inventoryHasItem(inventory.container(), item)) {
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
        Item item
    ) {
        List<BlockPos> positions = findInventoryPositionsWithItem(level, center, radius, player, item);
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

    private static boolean inventoryHasItem(Container inventory, Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    public record NearbyInventory(Container container, BlockPos key, List<BlockPos> positions) {
        public NearbyInventory {
            positions = List.copyOf(positions);
        }
    }

    public record NearbyItemEntry(ItemStack stack, int count) {
    }

    public record WorldPos(Level level, BlockPos pos) {
    }
}
