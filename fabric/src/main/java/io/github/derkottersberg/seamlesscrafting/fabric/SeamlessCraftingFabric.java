package io.github.derkottersberg.seamlesscrafting.fabric;

import com.derk.easyinventorycrafter.NearbyStorage;
import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import com.derk.easyinventorycrafter.net.NearbyHighlightRequestPacket;
import com.derk.easyinventorycrafter.net.NearbyHighlightResponsePacket;
import com.derk.easyinventorycrafter.net.NearbyItemsPacket;
import com.derk.easyinventorycrafter.net.RequestNearbyItemsPacket;
import com.derk.easyinventorycrafter.net.ReturnNearbyItemsPacket;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;
import io.github.derkottersberg.seamlesscrafting.internal.PlatformServices;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SeamlessCraftingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(RequestNearbyItemsPacket.TYPE, RequestNearbyItemsPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NearbyHighlightRequestPacket.TYPE, NearbyHighlightRequestPacket.STREAM_CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ReturnNearbyItemsPacket.TYPE, ReturnNearbyItemsPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NearbyItemsPacket.TYPE, NearbyItemsPacket.STREAM_CODEC);
        PayloadTypeRegistry.clientboundPlay().register(NearbyHighlightResponsePacket.TYPE, NearbyHighlightResponsePacket.STREAM_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(RequestNearbyItemsPacket.TYPE, (payload, context) ->
            context.server().execute(() -> EasyInventoryCrafterNetwork.handleRequestNearbyItems(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(NearbyHighlightRequestPacket.TYPE, (payload, context) ->
            context.server().execute(() -> EasyInventoryCrafterNetwork.handleHighlightRequest(context.player(), payload)));
        ServerPlayNetworking.registerGlobalReceiver(ReturnNearbyItemsPacket.TYPE, (payload, context) ->
            context.server().execute(() -> EasyInventoryCrafterNetwork.handleReturnNearbyItems(context.player(), payload)));

        SeamlessCraftingMod.initialize(new FabricPlatformServices());
    }

    private static final class FabricPlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Fabric";
        }

        @Override
        public Path configDirectory() {
            return FabricLoader.getInstance().getConfigDir();
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
            ServerPlayNetworking.send(player, payload);
        }

        @Override
        @Nullable
        public NearbyStorage findNearbyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, state, blockEntity, null);
            return storage == null ? null : new FabricNearbyStorage(storage, pos);
        }
    }
}
