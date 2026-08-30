package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import com.derk.easyinventorycrafter.NearbyInventoryScanner;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyInventory;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.WorldPos;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class NearbyItemsSync {
    private NearbyItemsSync() {
    }

    public static void sendNearbyItems(ServerPlayer player) {
        if (!(player.containerMenu instanceof CraftingMenu) && !(player.containerMenu instanceof InventoryMenu)) {
            return;
        }

        WorldPos worldPos = getWorldPos(player);
        if (worldPos == null) {
            return;
        }

        List<NearbyInventory> scanned = NearbyInventoryScanner.scan(
            worldPos.level(),
            worldPos.pos(),
            NearbyInventoryScanner.getConfiguredRadius(),
            player
        );
        List<Container> inventories = scanned.stream().map(NearbyInventory::container).toList();
        List<NearbyItemEntry> entries = NearbyInventoryScanner.collectItemCounts(inventories);
        List<ItemStack> recipeFinderStacks = NearbyInventoryScanner.collectRecipeFinderStacks(inventories);

        EasyInventoryCrafterNetwork.sendToPlayer(player, new NearbyItemsPacket(entries, recipeFinderStacks));
    }

    @Nullable
    private static ContainerLevelAccess getAccess(ServerPlayer player) {
        if (player.containerMenu instanceof NearbyCraftingAccess access) {
            return access.derk$getAccess();
        }
        return null;
    }

    @Nullable
    private static WorldPos getWorldPos(ServerPlayer player) {
        ContainerLevelAccess access = getAccess(player);
        if (access != null) {
            return access.evaluate((level, pos) -> new WorldPos(level, pos)).orElse(null);
        }

        if (player.containerMenu instanceof InventoryMenu) {
            return new WorldPos(player.level(), player.blockPosition());
        }

        return null;
    }

    @Nullable
    public static List<BlockPos> findHighlightPositions(ServerPlayer player, ItemStack stack) {
        WorldPos worldPos = getWorldPos(player);
        if (worldPos == null) {
            return null;
        }

        return NearbyInventoryScanner.findInventoryPositionsWithItem(
            worldPos.level(),
            worldPos.pos(),
            NearbyInventoryScanner.getConfiguredRadius(),
            player,
            stack.getItem()
        );
    }
}
