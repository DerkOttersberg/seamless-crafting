package com.derk.easyinventorycrafter.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record RequestNearbyItemsPacket() implements CustomPacketPayload {
    public static final Type<RequestNearbyItemsPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("request_nearby_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestNearbyItemsPacket> STREAM_CODEC = StreamCodec.unit(new RequestNearbyItemsPacket());

    public static RequestNearbyItemsPacket decode(RegistryFriendlyByteBuf buf) {
        return new RequestNearbyItemsPacket();
    }

    public static void encode(RequestNearbyItemsPacket packet, RegistryFriendlyByteBuf buf) {
    }

    @Override
    public Type<RequestNearbyItemsPacket> type() {
        return TYPE;
    }
}
