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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
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
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.RegisterEvent;
import org.jetbrains.annotations.Nullable;

@Mod(SeamlessCraftingMod.FORGE_ID)
public final class SeamlessCraftingForge {
    private static final String DEVELOPMENT_GAME_TESTS =
        "io.github.derkottersberg.seamlesscrafting.forge.gametest.SeamlessCraftingForgeGameTests";
    private static final Channel<CustomPacketPayload> NETWORK = createNetwork();

    public SeamlessCraftingForge(FMLJavaModLoadingContext context) {
        registerDevelopmentGameTests(context);
        SeamlessCraftingMod.initialize(new ForgePlatformServices());
        if (FMLEnvironment.dist.isClient()) {
            SeamlessCraftingForgeClient.initialize(context);
        }
    }

    /** Registers source-set-only GameTests without adding test classes to release jars. */
    private static void registerDevelopmentGameTests(FMLJavaModLoadingContext context) {
        Class<?> bootstrap;
        try {
            // This source-set-only class no longer touches registries during
            // initialization. Use Forge's caller-aware lookup because its
            // development content layer is not necessarily visible through the
            // transformed production class's defining loader.
            bootstrap = Class.forName(DEVELOPMENT_GAME_TESTS);
        } catch (ClassNotFoundException ignored) {
            // Expected for production jars and ordinary development launches.
            return;
        }

        RegisterEvent.getBus(context.getModBusGroup()).addListener(event -> {
            if (event.getRegistryKey() == Registries.TEST_FUNCTION) {
                registerDevelopmentGameTestFunctions(bootstrap, event);
            }
        });
    }

    private static void registerDevelopmentGameTestFunctions(Class<?> bootstrap, RegisterEvent event) {
        BiConsumer<String, Supplier<?>> registrar = (name, function) -> registerDevelopmentGameTest(
            event,
            name,
            function
        );
        try {
            // Keep Forge and Minecraft types out of the reflective method
            // descriptor. Development launch layers can transform those types
            // independently even when their binary names are identical.
            bootstrap.getMethod("registerAll", BiConsumer.class).invoke(null, registrar);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            throw new IllegalStateException("Could not register Seamless Crafting Forge GameTests", exception);
        } catch (InvocationTargetException exception) {
            if (exception.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (exception.getCause() instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Could not register Seamless Crafting Forge GameTests", exception.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    private static void registerDevelopmentGameTest(
        RegisterEvent event,
        String name,
        Supplier<?> function
    ) {
        if (name == null || name.isBlank() || function == null) {
            throw new IllegalArgumentException("Invalid Seamless Crafting Forge GameTest registration");
        }
        event.register(
            Registries.TEST_FUNCTION,
            Identifier.fromNamespaceAndPath(SeamlessCraftingMod.FORGE_ID, name),
            (Supplier<Consumer<GameTestHelper>>) function
        );
    }

    private static Channel<CustomPacketPayload> createNetwork() {
        return ChannelBuilder.named(SeamlessCraftingMod.networkId("network"))
            .networkProtocolVersion(EasyInventoryCrafterNetwork.PROTOCOL_VERSION)
            .payloadChannel()
            .play()
            .serverbound(flow -> flow
                .addMain(RequestNearbyItemsPacket.TYPE, RequestNearbyItemsPacket.STREAM_CODEC, (payload, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        EasyInventoryCrafterNetwork.handleRequestNearbyItems(sender, payload);
                    }
                })
                .addMain(NearbyHighlightRequestPacket.TYPE, NearbyHighlightRequestPacket.STREAM_CODEC, (payload, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        EasyInventoryCrafterNetwork.handleHighlightRequest(sender, payload);
                    }
                })
                .addMain(ReturnNearbyItemsPacket.TYPE, ReturnNearbyItemsPacket.STREAM_CODEC, (payload, context) -> {
                    ServerPlayer sender = context.getSender();
                    if (sender != null) {
                        EasyInventoryCrafterNetwork.handleReturnNearbyItems(sender, payload);
                    }
                }))
            .clientbound()
            .addMain(NearbyItemsPacket.TYPE, NearbyItemsPacket.STREAM_CODEC, (payload, context) -> {
                if (context.isClientSide()) {
                    SeamlessCraftingForgeClient.handleNearbyItems(payload);
                }
            })
            .addMain(NearbyHighlightResponsePacket.TYPE, NearbyHighlightResponsePacket.STREAM_CODEC, (payload, context) -> {
                if (context.isClientSide()) {
                    SeamlessCraftingForgeClient.handleHighlightResponse(payload);
                }
            })
            .build();
    }

    static void sendToServer(CustomPacketPayload payload) {
        NETWORK.send(payload, PacketDistributor.SERVER.noArg());
    }

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
        public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
            NETWORK.send(payload, PacketDistributor.PLAYER.with(player));
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
