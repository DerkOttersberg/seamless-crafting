package io.github.derkottersberg.seamlesscrafting.forge;

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
import com.derk.easyinventorycrafter.net.CommonPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.Optional;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

@Mod(SeamlessCraftingMod.FORGE_ID)
public final class SeamlessCraftingForge {
    private static final SimpleChannel NETWORK = createNetwork();

    public SeamlessCraftingForge() {
        FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
        SeamlessCraftingMod.initialize(new ForgePlatformServices());
        if (FMLEnvironment.dist.isClient()) {
            SeamlessCraftingForgeClient.initialize(context);
        }
    }

    private static SimpleChannel createNetwork() {
        String version = Integer.toString(EasyInventoryCrafterNetwork.PROTOCOL_VERSION);
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(SeamlessCraftingMod.networkId("network"),
            () -> version, version::equals, version::equals);
        channel.registerMessage(0, RequestNearbyItemsPacket.class,
            (payload, buffer) -> RequestNearbyItemsPacket.STREAM_CODEC.encode(buffer, payload),
            RequestNearbyItemsPacket.STREAM_CODEC::decode, (payload, supplier) -> {
                var context = supplier.get();
                context.enqueueWork(() -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) EasyInventoryCrafterNetwork.handleRequestNearbyItems(sender, payload);
                });
                context.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(1, NearbyHighlightRequestPacket.class,
            (payload, buffer) -> NearbyHighlightRequestPacket.STREAM_CODEC.encode(buffer, payload),
            NearbyHighlightRequestPacket.STREAM_CODEC::decode, (payload, supplier) -> {
                var context = supplier.get();
                context.enqueueWork(() -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) EasyInventoryCrafterNetwork.handleHighlightRequest(sender, payload);
                });
                context.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(2, ReturnNearbyItemsPacket.class,
            (payload, buffer) -> ReturnNearbyItemsPacket.STREAM_CODEC.encode(buffer, payload),
            ReturnNearbyItemsPacket.STREAM_CODEC::decode, (payload, supplier) -> {
                var context = supplier.get();
                context.enqueueWork(() -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) EasyInventoryCrafterNetwork.handleReturnNearbyItems(sender, payload);
                });
                context.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        channel.registerMessage(3, NearbyItemsPacket.class,
            (payload, buffer) -> NearbyItemsPacket.STREAM_CODEC.encode(buffer, payload),
            NearbyItemsPacket.STREAM_CODEC::decode, (payload, supplier) -> {
                var context = supplier.get();
                context.enqueueWork(() -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> SeamlessCraftingForgeClient.handleNearbyItems(payload));
                });
                context.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        channel.registerMessage(4, NearbyHighlightResponsePacket.class,
            (payload, buffer) -> NearbyHighlightResponsePacket.STREAM_CODEC.encode(buffer, payload),
            NearbyHighlightResponsePacket.STREAM_CODEC::decode, (payload, supplier) -> {
                var context = supplier.get();
                context.enqueueWork(() -> {
                    DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                        () -> () -> SeamlessCraftingForgeClient.handleHighlightResponse(payload));
                });
                context.setPacketHandled(true);
            }, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        return channel;
    }

    static void sendToServer(CommonPayload payload) { NETWORK.sendToServer(payload); }

    private static final class ForgePlatformServices implements PlatformServices {
        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public void sendToPlayer(ServerPlayer player, CommonPayload payload) {
            NETWORK.send(PacketDistributor.PLAYER.with(() -> player), payload);
        }

        @Override
        @Nullable
        public NearbyStorage findNearbyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity) {
            if (blockEntity == null) {
                return null;
            }
            IItemHandler handler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve().orElse(null);
            return handler == null ? null : new ForgeNearbyStorage(handler, pos);
        }
    }
}
