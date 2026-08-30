package io.github.derkottersberg.seamlesscrafting.forge;

import com.derk.easyinventorycrafter.client.EasyInventoryCrafterClient;
import com.derk.easyinventorycrafter.client.EasyInventoryCrafterClientNetwork;
import com.derk.easyinventorycrafter.client.EasyInventoryCrafterConfigScreen;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.derk.easyinventorycrafter.net.NearbyHighlightResponsePacket;
import com.derk.easyinventorycrafter.net.NearbyItemsPacket;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingClientBootstrap;
import io.github.derkottersberg.seamlesscrafting.internal.ClientPlatformServices;
import java.nio.file.Path;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

final class SeamlessCraftingForgeClient {
    private SeamlessCraftingForgeClient() {
    }

    static void initialize(FMLJavaModLoadingContext context) {
        SeamlessCraftingClientBootstrap.initialize(new ForgeClientPlatformServices());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event ->
            EasyInventoryCrafterClient.tick(Minecraft.getInstance()));
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> NearbyItemsClientState.clear());
        context.getContainer().registerExtensionPoint(
            ConfigScreenHandler.ConfigScreenFactory.class,
            () -> new ConfigScreenHandler.ConfigScreenFactory(EasyInventoryCrafterConfigScreen::new)
        );
    }

    static void handleNearbyItems(NearbyItemsPacket payload) {
        EasyInventoryCrafterClientNetwork.handleNearbyItems(payload);
    }

    static void handleHighlightResponse(NearbyHighlightResponsePacket payload) {
        EasyInventoryCrafterClientNetwork.handleHighlightResponse(payload);
    }

    private static final class ForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "Forge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public void sendToServer(CustomPacketPayload payload) {
            SeamlessCraftingForge.sendToServer(payload);
        }
    }
}
