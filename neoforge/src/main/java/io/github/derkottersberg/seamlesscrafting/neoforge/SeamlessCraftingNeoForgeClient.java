package io.github.derkottersberg.seamlesscrafting.neoforge;

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
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;

final class SeamlessCraftingNeoForgeClient {
    private SeamlessCraftingNeoForgeClient() {
    }

    static void initialize(ModContainer container) {
        SeamlessCraftingClientBootstrap.initialize(new NeoForgeClientPlatformServices());
        NeoForge.EVENT_BUS.addListener(SeamlessCraftingNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(SeamlessCraftingNeoForgeClient::onLogout);
        container.registerExtensionPoint(IConfigScreenFactory.class,
            (modContainer, parent) -> new EasyInventoryCrafterConfigScreen(parent));
    }

    static void handleNearbyItems(NearbyItemsPacket payload) {
        EasyInventoryCrafterClientNetwork.handleNearbyItems(payload);
    }

    static void handleHighlightResponse(NearbyHighlightResponsePacket payload) {
        EasyInventoryCrafterClientNetwork.handleHighlightResponse(payload);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        EasyInventoryCrafterClient.tick(Minecraft.getInstance());
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        NearbyItemsClientState.clear();
    }

    private static final class NeoForgeClientPlatformServices implements ClientPlatformServices {
        @Override
        public String loaderName() {
            return "NeoForge";
        }

        @Override
        public Path configDirectory() {
            return FMLPaths.CONFIGDIR.get();
        }

        @Override
        public void sendToServer(CustomPacketPayload payload) {
            ClientPacketDistributor.sendToServer(payload);
        }
    }
}
