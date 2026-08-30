package com.derk.easyinventorycrafter.net;

import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import io.github.derkottersberg.seamlesscrafting.SeamlessCraftingMod;

public record NearbyItemsPacket(List<NearbyItemEntry> entries, List<ItemStack> recipeFinderStacks) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 512;
    public static final int MAX_RECIPE_STACKS = 512;
    public static final Type<NearbyItemsPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("nearby_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyItemsPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> packet.write(buf), NearbyItemsPacket::decode);

    public static NearbyItemsPacket decode(RegistryFriendlyByteBuf buf) {
        int entryCount = buf.readVarInt();
        if (entryCount < 0 || entryCount > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid nearby item entry count: " + entryCount);
        }
        List<NearbyItemEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            int count = buf.readVarInt();
            entries.add(new NearbyItemEntry(stack, count));
        }

        int stackCount = buf.readVarInt();
        if (stackCount < 0 || stackCount > MAX_RECIPE_STACKS) {
            throw new IllegalArgumentException("Invalid nearby recipe stack count: " + stackCount);
        }
        List<ItemStack> recipeFinderStacks = new ArrayList<>(stackCount);
        for (int i = 0; i < stackCount; i++) {
            recipeFinderStacks.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(buf));
        }

        return new NearbyItemsPacket(entries, recipeFinderStacks);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        int entryCount = Math.min(entries.size(), MAX_ENTRIES);
        buf.writeVarInt(entryCount);
        for (NearbyItemEntry entry : entries.subList(0, entryCount)) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
            buf.writeVarInt(entry.count());
        }

        int stackCount = Math.min(recipeFinderStacks.size(), MAX_RECIPE_STACKS);
        buf.writeVarInt(stackCount);
        for (ItemStack stack : recipeFinderStacks.subList(0, stackCount)) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
    }

    @Override
    public Type<NearbyItemsPacket> type() {
        return TYPE;
    }
}
