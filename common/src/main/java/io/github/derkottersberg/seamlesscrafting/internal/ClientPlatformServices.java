package io.github.derkottersberg.seamlesscrafting.internal;

import java.nio.file.Path;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public interface ClientPlatformServices {
    String loaderName();

    Path configDirectory();

    void sendToServer(CustomPacketPayload payload);
}
