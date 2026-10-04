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
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.loader.api.FabricLoader;
import com.derk.easyinventorycrafter.net.CommonPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class SeamlessCraftingFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(RequestNearbyItemsPacket.TYPE.id(), (server, player, handler, buffer, response) -> {
            RequestNearbyItemsPacket payload = RequestNearbyItemsPacket.STREAM_CODEC.decode(buffer);
            server.execute(() -> EasyInventoryCrafterNetwork.handleRequestNearbyItems(player, payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(NearbyHighlightRequestPacket.TYPE.id(), (server, player, handler, buffer, response) -> {
            NearbyHighlightRequestPacket payload = NearbyHighlightRequestPacket.STREAM_CODEC.decode(buffer);
            server.execute(() -> EasyInventoryCrafterNetwork.handleHighlightRequest(player, payload));
        });
        ServerPlayNetworking.registerGlobalReceiver(ReturnNearbyItemsPacket.TYPE.id(), (server, player, handler, buffer, response) -> {
            ReturnNearbyItemsPacket payload = ReturnNearbyItemsPacket.STREAM_CODEC.decode(buffer);
            server.execute(() -> EasyInventoryCrafterNetwork.handleReturnNearbyItems(player, payload));
        });

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
        public void sendToPlayer(ServerPlayer player, CommonPayload payload) {
            if (!ServerPlayNetworking.canSend(player, payload.type().id())) return;
            var buffer = PacketByteBufs.create();
            if (payload instanceof NearbyItemsPacket items) NearbyItemsPacket.STREAM_CODEC.encode(buffer, items);
            else if (payload instanceof NearbyHighlightResponsePacket highlight) NearbyHighlightResponsePacket.STREAM_CODEC.encode(buffer, highlight);
            else throw new IllegalArgumentException("Not a clientbound Seamless Crafting packet");
            ServerPlayNetworking.send(player, payload.type().id(), buffer);
        }

        @Override
        @Nullable
        public NearbyStorage findNearbyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
            Storage<ItemVariant> storage = ItemStorage.SIDED.find(level, pos, state, blockEntity, null);
            return storage == null ? null : new FabricNearbyStorage(storage, pos);
        }
    }
}
