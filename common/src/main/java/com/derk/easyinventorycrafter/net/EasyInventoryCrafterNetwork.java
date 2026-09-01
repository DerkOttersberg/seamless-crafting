package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.NearbyCraftingAccess;
import io.github.derkottersberg.seamlesscrafting.internal.ClientPlatformServices;
import io.github.derkottersberg.seamlesscrafting.internal.PlatformServices;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;

public final class EasyInventoryCrafterNetwork {
    public static final int PROTOCOL_VERSION = 4;
    private static PlatformServices platform;
    private static ClientPlatformServices clientPlatform;

    private EasyInventoryCrafterNetwork() {
    }

    public static synchronized void initialize(PlatformServices services) {
        platform = Objects.requireNonNull(services, "services");
    }

    public static synchronized void initializeClient(ClientPlatformServices services) {
        clientPlatform = Objects.requireNonNull(services, "services");
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload packet) {
        PlatformServices services = platform;
        if (services == null) {
            throw new IllegalStateException("Server networking has not been initialized");
        }
        services.sendToPlayer(player, packet);
    }

    public static void sendToServer(CustomPacketPayload packet) {
        ClientPlatformServices services = clientPlatform;
        if (services == null) {
            throw new IllegalStateException("Client networking has not been initialized");
        }
        services.sendToServer(packet);
    }

    public static void handleRequestNearbyItems(ServerPlayer player, RequestNearbyItemsPacket packet) {
        if (isAuthorizedCraftingMenu(player)) {
            NearbyItemsSync.sendNearbyItems(player);
        }
    }

    public static void handleHighlightRequest(ServerPlayer player, NearbyHighlightRequestPacket packet) {
        if (!isAuthorizedCraftingMenu(player)) {
            return;
        }
        if (packet.stack().isEmpty()) {
            return;
        }

        List<net.minecraft.core.BlockPos> positions = NearbyItemsSync.findHighlightPositions(player, packet.stack());
        if (positions != null) {
            sendToPlayer(player, new NearbyHighlightResponsePacket(positions));
        }
    }

    public static void handleReturnNearbyItems(ServerPlayer player, ReturnNearbyItemsPacket packet) {
        if (isAuthorizedCraftingMenu(player) && player.containerMenu instanceof NearbyCraftingAccess access) {
            access.derk$cancelNearbyWithdrawals();
        }
    }

    private static boolean isAuthorizedCraftingMenu(ServerPlayer player) {
        return (player.containerMenu instanceof CraftingMenu || player.containerMenu instanceof InventoryMenu)
            && player.containerMenu.stillValid(player);
    }
}
