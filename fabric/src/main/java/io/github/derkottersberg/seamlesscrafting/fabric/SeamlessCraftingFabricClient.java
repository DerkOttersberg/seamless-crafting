package io.github.derkottersberg.seamlesscrafting.fabric;

import com.derk.easyinventorycrafter.client.EasyInventoryCrafterClient;
import com.derk.easyinventorycrafter.client.EasyInventoryCrafterClientNetwork;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.net.NearbyHighlightResponsePacket;
import com.derk.easyinventorycrafter.net.NearbyItemsPacket;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingClientBootstrap;
import io.github.derkottersberg.seamlesscrafting.internal.ClientPlatformServices;
import java.nio.file.Path;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public final class SeamlessCraftingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SeamlessCraftingClientBootstrap.initialize(new FabricClientPlatformServices());
        ClientPlayNetworking.registerGlobalReceiver(NearbyItemsPacket.TYPE, (payload, context) ->
            context.client().execute(() -> EasyInventoryCrafterClientNetwork.handleNearbyItems(payload)));
        ClientPlayNetworking.registerGlobalReceiver(NearbyHighlightResponsePacket.TYPE, (payload, context) ->
            context.client().execute(() -> EasyInventoryCrafterClientNetwork.handleHighlightResponse(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(EasyInventoryCrafterClient::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> NearbyItemsClientState.clear());
    }

    private static final class FabricClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public Path configDirectory() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public void sendToServer(CustomPacketPayload payload) {
            ClientPlayNetworking.send(payload);
        }
    }
}
