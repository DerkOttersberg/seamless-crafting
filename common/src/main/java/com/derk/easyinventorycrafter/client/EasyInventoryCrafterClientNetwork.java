package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.net.NearbyHighlightResponsePacket;
import com.derk.easyinventorycrafter.net.NearbyItemsPacket;

public final class EasyInventoryCrafterClientNetwork {
    private EasyInventoryCrafterClientNetwork() {
    }

    public static void handleNearbyItems(NearbyItemsPacket packet) {
        NearbyItemsClientState.applyPayload(packet);
    }

    public static void handleHighlightResponse(NearbyHighlightResponsePacket packet) {
        NearbyItemsClientState.setHighlight(packet.positions(), EasyInventoryCrafterConfig.getHighlightDurationTicks());
    }
}
