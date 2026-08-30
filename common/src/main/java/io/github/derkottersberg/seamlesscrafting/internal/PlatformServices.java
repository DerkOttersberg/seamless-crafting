package io.github.derkottersberg.seamlesscrafting.internal;

import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public interface PlatformServices {
    String loaderName();

    Path configDirectory();

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);
}
