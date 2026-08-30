package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

public final class EasyInventoryCrafterClient {
    private EasyInventoryCrafterClient() {
    }

    public static void tick(Minecraft client) {
        NearbyItemsClientState.tickHighlight(client);

        if (!(client.gui.screen() instanceof CraftingScreen) && !(client.gui.screen() instanceof InventoryScreen)) {
            NearbyItemsClientState.resetAutoRefreshCounter();
            return;
        }

        NearbyItemsClientState.incrementAutoRefreshCounter();
        if (NearbyItemsClientState.shouldAutoRefresh(EasyInventoryCrafterConfig.getAutoRefreshTicks())) {
            NearbyItemsClientState.requestUpdate();
            NearbyItemsClientState.resetAutoRefreshCounter();
        }
    }
}
