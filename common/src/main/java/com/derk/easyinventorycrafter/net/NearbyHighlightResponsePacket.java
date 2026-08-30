package com.derk.easyinventorycrafter.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record NearbyHighlightResponsePacket(List<BlockPos> positions) implements CustomPacketPayload {
    public static final int MAX_POSITIONS = 512;
    public static final Type<NearbyHighlightResponsePacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("nearby_highlight_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyHighlightResponsePacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> packet.write(buf), NearbyHighlightResponsePacket::decode);

    public static NearbyHighlightResponsePacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_POSITIONS) {
            throw new IllegalArgumentException("Invalid nearby highlight position count: " + size);
        }
        List<BlockPos> positions = new ArrayList<>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            positions.add(buf.readBlockPos());
        }
        return new NearbyHighlightResponsePacket(positions);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        int size = Math.min(positions.size(), MAX_POSITIONS);
        buf.writeVarInt(size);
        for (BlockPos pos : positions.subList(0, size)) {
            buf.writeBlockPos(pos);
        }
    }

    @Override
    public Type<NearbyHighlightResponsePacket> type() {
        return TYPE;
    }
}
