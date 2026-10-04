package com.derk.easyinventorycrafter.net;

import net.minecraft.resources.ResourceLocation;

/** Loader-neutral message identity for the 1.20.1 networking adapters. */
public interface CommonPayload {
    record Type<T extends CommonPayload>(ResourceLocation id) {}
    Type<? extends CommonPayload> type();
}
