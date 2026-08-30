package com.derk.easyinventorycrafter.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record NearbyHighlightRequestPacket(ItemStack stack) implements CustomPacketPayload {
    public static final Type<NearbyHighlightRequestPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("nearby_highlight_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyHighlightRequestPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> packet.write(buf), NearbyHighlightRequestPacket::decode);

    public static NearbyHighlightRequestPacket decode(RegistryFriendlyByteBuf buf) {
        return new NearbyHighlightRequestPacket(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
    }

    @Override
    public Type<NearbyHighlightRequestPacket> type() {
        return TYPE;
    }
}
