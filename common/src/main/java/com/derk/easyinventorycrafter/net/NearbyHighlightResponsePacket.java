package com.derk.easyinventorycrafter.net;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import com.derk.easyinventorycrafter.net.PacketCodec;
import com.derk.easyinventorycrafter.net.CommonPayload;
import net.minecraft.resources.ResourceLocation;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record NearbyHighlightResponsePacket(List<BlockPos> positions) implements CommonPayload {
    public static final int MAX_POSITIONS = 512;
    public static final Type<NearbyHighlightResponsePacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("nearby_highlight_response"));
    public static final PacketCodec<FriendlyByteBuf, NearbyHighlightResponsePacket> STREAM_CODEC = PacketCodec.of((buf, packet) -> packet.write(buf), NearbyHighlightResponsePacket::decode);

    public NearbyHighlightResponsePacket {
        positions = List.copyOf(positions);
        if (positions.size() > MAX_POSITIONS) {
            throw new IllegalArgumentException("Too many nearby highlight positions: " + positions.size());
        }
    }

    public static NearbyHighlightResponsePacket decode(FriendlyByteBuf buf) {
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

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(positions.size());
        for (BlockPos pos : positions) {
            buf.writeBlockPos(pos);
        }
    }

    @Override
    public Type<NearbyHighlightResponsePacket> type() {
        return TYPE;
    }
}
