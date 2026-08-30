package io.github.derkottersberg.seamlesscrafting.neoforge;

import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import com.derk.easyinventorycrafter.net.NearbyHighlightRequestPacket;
import com.derk.easyinventorycrafter.net.NearbyHighlightResponsePacket;
import com.derk.easyinventorycrafter.net.NearbyItemsPacket;
import com.derk.easyinventorycrafter.net.RequestNearbyItemsPacket;
import com.derk.easyinventorycrafter.net.ReturnNearbyItemsPacket;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;
import io.github.derkottersberg.seamlesscrafting.internal.PlatformServices;
import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

@Mod(SeamlessCraftingMod.FORGE_ID)
public final class SeamlessCraftingNeoForge {
    public SeamlessCraftingNeoForge(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(this::registerPayloads);
        SeamlessCraftingMod.initialize(new NeoForgePlatformServices());
        if (FMLEnvironment.getDist().isClient()) {
            SeamlessCraftingNeoForgeClient.initialize(container);
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(Integer.toString(EasyInventoryCrafterNetwork.PROTOCOL_VERSION))
            .executesOn(HandlerThread.MAIN)
            .playToServer(RequestNearbyItemsPacket.TYPE, RequestNearbyItemsPacket.STREAM_CODEC,
                (payload, context) -> EasyInventoryCrafterNetwork.handleRequestNearbyItems((ServerPlayer) context.player(), payload))
            .playToServer(NearbyHighlightRequestPacket.TYPE, NearbyHighlightRequestPacket.STREAM_CODEC,
                (payload, context) -> EasyInventoryCrafterNetwork.handleHighlightRequest((ServerPlayer) context.player(), payload))
            .playToServer(ReturnNearbyItemsPacket.TYPE, ReturnNearbyItemsPacket.STREAM_CODEC,
                (payload, context) -> EasyInventoryCrafterNetwork.handleReturnNearbyItems((ServerPlayer) context.player(), payload))
            .playToClient(NearbyItemsPacket.TYPE, NearbyItemsPacket.STREAM_CODEC,
                (payload, context) -> SeamlessCraftingNeoForgeClient.handleNearbyItems(payload))
            .playToClient(NearbyHighlightResponsePacket.TYPE, NearbyHighlightResponsePacket.STREAM_CODEC,
                (payload, context) -> SeamlessCraftingNeoForgeClient.handleHighlightResponse(payload));
    }

    private static final class NeoForgePlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
