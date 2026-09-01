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

public record NearbyItemsPacket(
    List<NearbyItemEntry> entries,
    List<ItemStack> recipeFinderStacks,
    boolean truncated
) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 512;
    public static final int MAX_RECIPE_STACKS = 512;
    public static final long MAX_REPORTED_COUNT = com.derk.easyinventorycrafter.NearbyInventoryScanner.MAX_REPORTED_COUNT;
    public static final Type<NearbyItemsPacket> TYPE = new Type<>(SeamlessCraftingMod.networkId("nearby_items"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NearbyItemsPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> packet.write(buf), NearbyItemsPacket::decode);

    public NearbyItemsPacket {
        entries = List.copyOf(entries);
        recipeFinderStacks = recipeFinderStacks.stream().map(ItemStack::copy).toList();
        if (entries.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException("Too many nearby item entries: " + entries.size());
        }
        if (recipeFinderStacks.size() > MAX_RECIPE_STACKS) {
            throw new IllegalArgumentException("Too many nearby recipe stacks: " + recipeFinderStacks.size());
        }
        for (NearbyItemEntry entry : entries) {
            if (entry.count() > MAX_REPORTED_COUNT) {
                throw new IllegalArgumentException("Nearby item count exceeds the wire bound");
            }
        }
        if (recipeFinderStacks.stream().anyMatch(ItemStack::isEmpty)) {
            throw new IllegalArgumentException("Nearby recipe stacks must not be empty");
        }
    }

    public static NearbyItemsPacket decode(RegistryFriendlyByteBuf buf) {
        int entryCount = buf.readVarInt();
        if (entryCount < 0 || entryCount > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid nearby item entry count: " + entryCount);
        }
        List<NearbyItemEntry> entries = new ArrayList<>(entryCount);
        for (int i = 0; i < entryCount; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            long count = buf.readVarLong();
            if (stack.isEmpty() || count <= 0 || count > MAX_REPORTED_COUNT) {
                throw new IllegalArgumentException("Invalid nearby item entry");
            }
            entries.add(new NearbyItemEntry(stack, count));
        }

        int stackCount = buf.readVarInt();
        if (stackCount < 0 || stackCount > MAX_RECIPE_STACKS) {
            throw new IllegalArgumentException("Invalid nearby recipe stack count: " + stackCount);
        }
        List<ItemStack> recipeFinderStacks = new ArrayList<>(stackCount);
        for (int i = 0; i < stackCount; i++) {
            ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("Empty nearby recipe stack");
            }
            recipeFinderStacks.add(stack);
        }

        boolean truncated = buf.readBoolean();
        return new NearbyItemsPacket(entries, recipeFinderStacks, truncated);
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (NearbyItemEntry entry : entries) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, entry.stack());
            buf.writeVarLong(entry.count());
        }

        buf.writeVarInt(recipeFinderStacks.size());
        for (ItemStack stack : recipeFinderStacks) {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, stack);
        }
        buf.writeBoolean(truncated);
    }

    @Override
    public Type<NearbyItemsPacket> type() {
        return TYPE;
    }
}
