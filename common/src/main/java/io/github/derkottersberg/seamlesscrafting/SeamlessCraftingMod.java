package io.github.derkottersberg.seamlesscrafting;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import io.github.derkottersberg.seamlesscrafting.internal.PlatformServices;
import java.util.Objects;
import net.minecraft.resources.Identifier;

public final class SeamlessCraftingMod {
    /** Fabric's established public mod id and the canonical 2.0 name. */
    public static final String CANONICAL_ID = "seamless_crafting";
    /** Forge and NeoForge keep this established id for installation compatibility. */
    public static final String FORGE_ID = "derk_easy_inventory_crafter";
    /** Packet ids retain the Forge/NeoForge namespace used by the feature superset. */
    public static final String NETWORK_ID = FORGE_ID;

    private static PlatformServices platform;

    private SeamlessCraftingMod() {
    }

    public static synchronized void initialize(PlatformServices services) {
        Objects.requireNonNull(services, "services");
        if (platform != null) {
            if (platform != services) {
                throw new IllegalStateException("Seamless Crafting was initialized more than once");
            }
            return;
        }

        platform = services;
        EasyInventoryCrafterConfig.load(services.configDirectory());
        EasyInventoryCrafterNetwork.initialize(services);
    }

    public static PlatformServices platform() {
        if (platform == null) {
            throw new IllegalStateException("Seamless Crafting has not been initialized");
        }
        return platform;
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(CANONICAL_ID, path);
    }

    public static Identifier networkId(String path) {
        return Identifier.fromNamespaceAndPath(NETWORK_ID, path);
    }
}
