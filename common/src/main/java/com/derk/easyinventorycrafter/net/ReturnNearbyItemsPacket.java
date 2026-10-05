package com.derk.easyinventorycrafter.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.derk.easyinventorycrafter.net.PacketCodec;
import com.derk.easyinventorycrafter.net.CommonPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record ReturnNearbyItemsPacket() implements CommonPayload {
    public static final Type<ReturnNearbyItemsPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("return_nearby_items"));
    public static final PacketCodec<RegistryFriendlyByteBuf, ReturnNearbyItemsPacket> STREAM_CODEC = PacketCodec.unit(new ReturnNearbyItemsPacket());

    public static ReturnNearbyItemsPacket decode(RegistryFriendlyByteBuf buf) {
        return new ReturnNearbyItemsPacket();
    }

    public static void encode(ReturnNearbyItemsPacket packet, RegistryFriendlyByteBuf buf) {
    }

    @Override
    public Type<ReturnNearbyItemsPacket> type() {
        return TYPE;
    }
}
