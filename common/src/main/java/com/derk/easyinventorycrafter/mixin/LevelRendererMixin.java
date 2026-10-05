package com.derk.easyinventorycrafter.mixin;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.client.NearbyItemsClientState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.derkottersberg.seamlesscrafting.client.render.HighlightBox;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Submits highlights through Minecraft's backend-neutral feature pipeline. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    private static final float HIGHLIGHT_FACE_OFFSET = 0.003f;
    private static final float DISTANCE_LABEL_HEIGHT = 1.02f;

    @Inject(method = "renderLevel", at = @At("TAIL"))
    private void derk$renderHighlights(net.minecraft.client.DeltaTracker delta,
            boolean outlines, Camera activeCamera, GameRenderer renderer, LightTexture lightTexture,
            Matrix4f modelView, Matrix4f projection, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = new PoseStack();
        pose.mulPose(modelView);
        if (!NearbyItemsClientState.hasHighlight() || minecraft.level == null) return;
        List<BlockPos> positions = List.copyOf(NearbyItemsClientState.getHighlightPositions());
        if (positions.isEmpty()) return;
        Vec3 camera = activeCamera.getPosition();
        float fade = NearbyItemsClientState.getHighlightAlpha();
        int rgb = EasyInventoryCrafterConfig.getHighlightColor();
        int alpha = Math.max(0, Math.min(255, Math.round(fade * EasyInventoryCrafterConfig.getHighlightOpacity() * 255.0f)));
        int color = (alpha << 24) | (rgb & 0xFFFFFF);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.debugQuads());
        for (HighlightBox box : derk$extractBoxes(minecraft, positions, camera)) {
            derk$renderFilledBoxFaces(pose.last().pose(), consumer, box, color);
        }
        buffers.endBatch(RenderType.debugQuads());
        if (EasyInventoryCrafterConfig.isDistanceLabelEnabled() && minecraft.player != null) {
            derk$submitDistanceLabels(minecraft, buffers, pose, positions, camera, fade);
            buffers.endBatch();
        }
    }

    private static List<HighlightBox> derk$extractBoxes(Minecraft minecraft, List<BlockPos> positions, Vec3 camera) {
        List<HighlightBox> boxes = new ArrayList<>();
        for (BlockPos pos : positions) {
            if (minecraft.level.isOutsideBuildHeight(pos)) {
                continue;
            }
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getShape(minecraft.level, pos, CollisionContext.empty());
            double dx = pos.getX() - camera.x;
            double dy = pos.getY() - camera.y;
            double dz = pos.getZ() - camera.z;
            shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) -> boxes.add(new HighlightBox(
                (float) (dx + minX),
                (float) (dy + minY),
                (float) (dz + minZ),
                (float) (dx + maxX),
                (float) (dy + maxY),
                (float) (dz + maxZ)
            )));
        }
        return List.copyOf(boxes);
    }

    private static void derk$submitDistanceLabels(
        Minecraft minecraft,
        MultiBufferSource.BufferSource buffers,
        PoseStack worldPose,
        List<BlockPos> positions,
        Vec3 camera,
        float fade
    ) {
        Set<BlockPos> labeled = new HashSet<>();
        Set<BlockPos> highlighted = new HashSet<>(positions);
        Vec3 eye = minecraft.player.getEyePosition();

        for (BlockPos pos : positions) {
            if (!labeled.add(pos) || minecraft.level.isOutsideBuildHeight(pos)) {
                continue;
            }
            BlockState state = minecraft.level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            Vec3 anchor = Vec3.atBottomCenterOf(pos);
            if (state.getBlock() instanceof ChestBlock && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                BlockPos connected = pos.relative(ChestBlock.getConnectedDirection(state));
                if (highlighted.contains(connected) && minecraft.level.isLoaded(connected)) {
                    labeled.add(connected);
                    anchor = new Vec3(
                        (pos.getX() + connected.getX()) / 2.0 + 0.5,
                        Math.max(pos.getY(), connected.getY()),
                        (pos.getZ() + connected.getZ()) / 2.0 + 0.5
                    );
                }
            }

            String text = String.format(Locale.ROOT, "%.1fm", Math.sqrt(eye.distanceToSqr(anchor)));
            int textAlpha = Math.max(64, Math.min(255, Math.round(fade * 255.0f)));
            int textColor = (textAlpha << 24) | 0xFFFFFF;
            int backgroundColor = Math.max(48, textAlpha / 2) << 24;
            float textX = -minecraft.font.width(text) / 2.0f;
            float yaw = (float) Math.toDegrees(Math.atan2(eye.x - anchor.x, eye.z - anchor.z)) + 180.0f;

            worldPose.pushPose();
            worldPose.translate(anchor.x - camera.x, anchor.y - camera.y + DISTANCE_LABEL_HEIGHT, anchor.z - camera.z);
            worldPose.mulPose(Axis.YP.rotationDegrees(yaw));
            worldPose.scale(-0.025f, -0.025f, 0.025f);
            minecraft.font.drawInBatch(text, textX, 0, textColor, false, worldPose.last().pose(),
                buffers, Font.DisplayMode.SEE_THROUGH, backgroundColor, 15728880);
            worldPose.popPose();
        }
    }

    private static void derk$renderFilledBoxFaces(
        Matrix4f matrix,
        VertexConsumer consumer,
        HighlightBox box,
        int color
    ) {
        float minX = box.minX() - HIGHLIGHT_FACE_OFFSET;
        float minY = box.minY() - HIGHLIGHT_FACE_OFFSET;
        float minZ = box.minZ() - HIGHLIGHT_FACE_OFFSET;
        float maxX = box.maxX() + HIGHLIGHT_FACE_OFFSET;
        float maxY = box.maxY() + HIGHLIGHT_FACE_OFFSET;
        float maxZ = box.maxZ() + HIGHLIGHT_FACE_OFFSET;

        derk$addQuad(consumer, matrix, minX, box.minY(), minZ, maxX, box.minY(), minZ, maxX, box.maxY(), minZ, minX, box.maxY(), minZ, color);
        derk$addQuad(consumer, matrix, maxX, box.minY(), maxZ, minX, box.minY(), maxZ, minX, box.maxY(), maxZ, maxX, box.maxY(), maxZ, color);
        derk$addQuad(consumer, matrix, minX, box.minY(), maxZ, minX, box.minY(), minZ, minX, box.maxY(), minZ, minX, box.maxY(), maxZ, color);
        derk$addQuad(consumer, matrix, maxX, box.minY(), minZ, maxX, box.minY(), maxZ, maxX, box.maxY(), maxZ, maxX, box.maxY(), minZ, color);
        derk$addQuad(consumer, matrix, box.minX(), maxY, box.minZ(), box.maxX(), maxY, box.minZ(), box.maxX(), maxY, box.maxZ(), box.minX(), maxY, box.maxZ(), color);
        derk$addQuad(consumer, matrix, box.minX(), minY, box.maxZ(), box.maxX(), minY, box.maxZ(), box.maxX(), minY, box.minZ(), box.minX(), minY, box.minZ(), color);
    }

    private static void derk$addQuad(
        VertexConsumer consumer,
        Matrix4f matrix,
        float x1, float y1, float z1,
        float x2, float y2, float z2,
        float x3, float y3, float z3,
        float x4, float y4, float z4,
        int color
    ) {
        consumer.addVertex(matrix, x1, y1, z1).setColor(color);
        consumer.addVertex(matrix, x2, y2, z2).setColor(color);
        consumer.addVertex(matrix, x3, y3, z3).setColor(color);
        consumer.addVertex(matrix, x4, y4, z4).setColor(color);
    }
}
