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
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import com.derk.easyinventorycrafter.net.RequestNearbyItemsPacket;
import com.derk.easyinventorycrafter.net.NearbyHighlightRequestPacket;
import com.derk.easyinventorycrafter.net.ReturnNearbyItemsPacket;
import net.fabricmc.loader.api.FabricLoader;
import com.derk.easyinventorycrafter.net.CommonPayload;

public final class SeamlessCraftingFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SeamlessCraftingClientBootstrap.initialize(new FabricClientPlatformServices());
        ClientPlayNetworking.registerGlobalReceiver(NearbyItemsPacket.TYPE.id(), (client, handler, buffer, response) -> {
            NearbyItemsPacket payload = NearbyItemsPacket.STREAM_CODEC.decode(buffer);
            client.execute(() -> EasyInventoryCrafterClientNetwork.handleNearbyItems(payload));
        });
        ClientPlayNetworking.registerGlobalReceiver(NearbyHighlightResponsePacket.TYPE.id(), (client, handler, buffer, response) -> {
            NearbyHighlightResponsePacket payload = NearbyHighlightResponsePacket.STREAM_CODEC.decode(buffer);
            client.execute(() -> EasyInventoryCrafterClientNetwork.handleHighlightResponse(payload));
        });
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
        public void sendToServer(CommonPayload payload) {
            if (!ClientPlayNetworking.canSend(payload.type().id())) return;
            var buffer = PacketByteBufs.create();
            if (payload instanceof RequestNearbyItemsPacket request) RequestNearbyItemsPacket.STREAM_CODEC.encode(buffer, request);
            else if (payload instanceof NearbyHighlightRequestPacket highlight) NearbyHighlightRequestPacket.STREAM_CODEC.encode(buffer, highlight);
            else if (payload instanceof ReturnNearbyItemsPacket returns) ReturnNearbyItemsPacket.STREAM_CODEC.encode(buffer, returns);
            else throw new IllegalArgumentException("Not a serverbound Seamless Crafting packet");
            ClientPlayNetworking.send(payload.type().id(), buffer);
        }
    }
}
