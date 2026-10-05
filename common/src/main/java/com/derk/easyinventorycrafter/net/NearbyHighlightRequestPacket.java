package com.derk.easyinventorycrafter.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import com.derk.easyinventorycrafter.net.PacketCodec;
import com.derk.easyinventorycrafter.net.CommonPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record NearbyHighlightRequestPacket(ItemStack stack) implements CommonPayload {
    public static final Type<NearbyHighlightRequestPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("nearby_highlight_request"));
    public static final PacketCodec<RegistryFriendlyByteBuf, NearbyHighlightRequestPacket> STREAM_CODEC = PacketCodec.of((buf, packet) -> packet.write(buf), NearbyHighlightRequestPacket::decode);

    public static NearbyHighlightRequestPacket decode(RegistryFriendlyByteBuf buf) {
        return new NearbyHighlightRequestPacket(ItemStack.STREAM_CODEC.decode(buf));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        ItemStack.STREAM_CODEC.encode(buf, stack);
    }

    @Override
    public Type<NearbyHighlightRequestPacket> type() {
        return TYPE;
    }
}
