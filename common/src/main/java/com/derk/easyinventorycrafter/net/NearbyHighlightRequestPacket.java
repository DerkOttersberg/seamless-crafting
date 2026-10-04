package com.derk.easyinventorycrafter.net;

import net.minecraft.network.FriendlyByteBuf;
import com.derk.easyinventorycrafter.net.PacketCodec;
import com.derk.easyinventorycrafter.net.CommonPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record NearbyHighlightRequestPacket(ItemStack stack) implements CommonPayload {
    public static final Type<NearbyHighlightRequestPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("nearby_highlight_request"));
    public static final PacketCodec<FriendlyByteBuf, NearbyHighlightRequestPacket> STREAM_CODEC = PacketCodec.of((buf, packet) -> packet.write(buf), NearbyHighlightRequestPacket::decode);

    public static NearbyHighlightRequestPacket decode(FriendlyByteBuf buf) {
        return new NearbyHighlightRequestPacket(buf.readItem());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeItem(stack);
    }

    @Override
    public Type<NearbyHighlightRequestPacket> type() {
        return TYPE;
    }
}
