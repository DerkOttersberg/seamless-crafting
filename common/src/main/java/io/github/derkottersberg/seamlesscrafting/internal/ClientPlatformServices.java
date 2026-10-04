package io.github.derkottersberg.seamlesscrafting.internal;

import java.nio.file.Path;
import com.derk.easyinventorycrafter.net.CommonPayload;

public interface ClientPlatformServices {
    String loaderName();

    Path configDirectory();

    void sendToServer(CommonPayload payload);
}
