package com.derk.easyinventorycrafter.net;

import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;

/** Explicit buffer codec; does not expose any loader-owned networking type. */
public interface PacketCodec<B extends FriendlyByteBuf, T> {
    void encode(B buffer, T value);
    T decode(B buffer);
    static <B extends FriendlyByteBuf, T> PacketCodec<B, T> of(BiConsumer<B, T> encoder, Function<B, T> decoder) {
        return new PacketCodec<>() {
            public void encode(B buffer, T value) { encoder.accept(buffer, value); }
            public T decode(B buffer) { return decoder.apply(buffer); }
        };
    }
    static <B extends FriendlyByteBuf, T> PacketCodec<B, T> unit(T value) {
        return of((buffer, ignored) -> {}, buffer -> value);
    }
}
