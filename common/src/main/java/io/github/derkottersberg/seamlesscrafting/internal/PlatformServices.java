package io.github.derkottersberg.seamlesscrafting.internal;

import java.nio.file.Path;
import com.derk.easyinventorycrafter.NearbyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface PlatformServices {
    String loaderName();

    Path configDirectory();

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    /** Returns the loader's standard automation-facing item storage, if any. */
    @Nullable
    NearbyStorage findNearbyStorage(Level level, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity);
}
