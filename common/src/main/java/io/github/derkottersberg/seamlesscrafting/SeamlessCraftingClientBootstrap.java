package io.github.derkottersberg.seamlesscrafting;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import io.github.derkottersberg.seamlesscrafting.internal.ClientPlatformServices;
import java.util.Objects;

public final class SeamlessCraftingClientBootstrap {
    private static ClientPlatformServices platform;

    private SeamlessCraftingClientBootstrap() {
    }

    public static synchronized void initialize(ClientPlatformServices services) {
        Objects.requireNonNull(services, "services");
        if (platform != null) {
            if (platform != services) {
                throw new IllegalStateException("Seamless Crafting client was initialized more than once");
            }
            return;
        }

        platform = services;
        EasyInventoryCrafterConfig.load(services.configDirectory());
        EasyInventoryCrafterNetwork.initializeClient(services);
    }

    public static ClientPlatformServices platform() {
        if (platform == null) {
            throw new IllegalStateException("Seamless Crafting client has not been initialized");
        }
        return platform;
    }
}
