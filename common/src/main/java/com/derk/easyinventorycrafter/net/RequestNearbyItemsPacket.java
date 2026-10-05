package com.derk.easyinventorycrafter.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.derk.easyinventorycrafter.net.PacketCodec;
import com.derk.easyinventorycrafter.net.CommonPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record RequestNearbyItemsPacket() implements CommonPayload {
    public static final Type<RequestNearbyItemsPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("request_nearby_items"));
    public static final PacketCodec<RegistryFriendlyByteBuf, RequestNearbyItemsPacket> STREAM_CODEC = PacketCodec.unit(new RequestNearbyItemsPacket());

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
