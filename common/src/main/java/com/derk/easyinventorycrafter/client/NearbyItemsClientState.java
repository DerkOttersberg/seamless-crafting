package com.derk.easyinventorycrafter.client;

import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig;
import com.derk.easyinventorycrafter.EasyInventoryCrafterConfig.LocateTrailParticle;
import com.derk.easyinventorycrafter.NearbyInventoryScanner.NearbyItemEntry;
import com.derk.easyinventorycrafter.net.EasyInventoryCrafterNetwork;
import com.derk.easyinventorycrafter.net.NearbyHighlightRequestPacket;
import com.derk.easyinventorycrafter.net.NearbyItemsPacket;
import com.derk.easyinventorycrafter.net.RequestNearbyItemsPacket;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class NearbyItemsClientState {
    private static final double SMOKE_TRAIL_MIN_DISTANCE_SQUARED = 81.0;
    private static final int SMOKE_TRAIL_TICKS = 14;
    private static final int SMOKE_PARTICLES_PER_TICK = 10;

    private static List<NearbyItemEntry> entries = Collections.emptyList();
    private static List<ItemStack> recipeFinderStacks = Collections.emptyList();
    private static List<BlockPos> highlightPositions = Collections.emptyList();
    private static int highlightTicks;
    private static int highlightTotalTicks;
    private static boolean locateFeedbackPending;
    private static Vec3 smokeTrailStart = Vec3.ZERO;
    private static Vec3 smokeTrailTarget = Vec3.ZERO;
    private static int smokeTrailTicks;
    private static double smokeTrailProgress;
    private static int autoRefreshCounter;
    private static boolean loading;
    private static boolean receivedPayload;
    private static boolean truncated;

    private NearbyItemsClientState() {
    }

    public static List<NearbyItemEntry> getEntries() {
        return entries;
    }

    public static List<ItemStack> getRecipeFinderStacks() {
        return recipeFinderStacks;
    }

    public static boolean isLoading() {
        return loading;
    }

    public static boolean hasReceivedPayload() {
        return receivedPayload;
    }

    public static boolean isTruncated() {
        return truncated;
    }

    public static void clear() {
        entries = Collections.emptyList();
        recipeFinderStacks = Collections.emptyList();
        highlightPositions = Collections.emptyList();
        highlightTicks = 0;
        highlightTotalTicks = 0;
        locateFeedbackPending = false;
        smokeTrailTicks = 0;
        smokeTrailProgress = 0.0;
        smokeTrailStart = Vec3.ZERO;
        smokeTrailTarget = Vec3.ZERO;
        autoRefreshCounter = 0;
        loading = false;
        receivedPayload = false;
        truncated = false;
    }

    public static void requestUpdate() {
        loading = true;
        EasyInventoryCrafterNetwork.sendToServer(new RequestNearbyItemsPacket());
    }

    public static void applyPayload(NearbyItemsPacket payload) {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            entries = List.copyOf(payload.entries());
            recipeFinderStacks = List.copyOf(payload.recipeFinderStacks());
            truncated = payload.truncated();
            receivedPayload = true;
            loading = false;
            if (client.gui.screen() instanceof RecipeUpdateListener listener) {
                if (client.gui.screen() instanceof NearbyRecipeBookRefreshAccess access) {
                    access.derk$refreshNearbyRecipeBook();
                }
                listener.recipesUpdated();
            }
        });
    }

    public static void requestHighlight(ItemStack stack) {
        requestHighlight(stack, false);
    }

    public static void requestHighlightAndAim(ItemStack stack) {
        requestHighlight(stack, true);
    }

    private static void requestHighlight(ItemStack stack, boolean aimAfterResponse) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        locateFeedbackPending = aimAfterResponse;
        EasyInventoryCrafterNetwork.sendToServer(new NearbyHighlightRequestPacket(stack.copyWithCount(1)));
    }

    public static void setHighlight(List<BlockPos> positions, int ticks) {
        highlightPositions = positions == null ? Collections.emptyList() : positions;
        highlightTicks = ticks;
        highlightTotalTicks = ticks;
        if (locateFeedbackPending) {
            locateFeedbackPending = false;
            Vec3 target = getNearestHighlightCenter();
            if (target != null) {
                if (EasyInventoryCrafterConfig.isSnapAimEnabled()) {
                    aimAtTarget(target);
                }
                if (EasyInventoryCrafterConfig.isLocateTrailEnabled()) {
                    startSmokeTrail(target);
                }
            }
        }
    }

    public static List<BlockPos> getHighlightPositions() {
        return highlightPositions;
    }

    public static boolean hasHighlight() {
        return EasyInventoryCrafterConfig.isHighlightEnabled() && !highlightPositions.isEmpty() && highlightTicks > 0;
    }

    public static float getHighlightAlpha() {
        if (highlightTotalTicks <= 0) {
            return 1.0f;
        }
        float progress = 1.0f - (highlightTicks / (float) highlightTotalTicks);
        float alpha = (float) Math.sin(Math.PI * progress);
        return Math.max(0.0f, Math.min(1.0f, alpha));
    }

    public static void tickHighlight(Minecraft client) {
        tickSmokeTrail(client);
        if (highlightPositions.isEmpty() || highlightTicks <= 0 || client.level == null) {
            if (highlightTicks <= 0) {
                highlightPositions = Collections.emptyList();
                highlightTotalTicks = 0;
            }
            return;
        }

        highlightTicks--;
        if (highlightTicks <= 0) {
            highlightPositions = Collections.emptyList();
            highlightTotalTicks = 0;
        }
    }

    private static Vec3 getNearestHighlightCenter() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || highlightPositions.isEmpty()) {
            return null;
        }

        Vec3 eyePos = client.player.getEyePosition();
        BlockPos nearestPos = null;
        double nearestDistance = Double.MAX_VALUE;
        for (BlockPos pos : highlightPositions) {
            double distance = eyePos.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPos = pos;
            }
        }

        if (nearestPos == null) {
            return null;
        }
        return new Vec3(nearestPos.getX() + 0.5, nearestPos.getY() + 0.5, nearestPos.getZ() + 0.5);
    }

    private static void aimAtTarget(Vec3 target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        Vec3 eyePos = client.player.getEyePosition();
        Vec3 delta = target.subtract(eyePos);
        double horizontalDistance = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
        float yaw = (float) (Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(delta.y, horizontalDistance)));
        client.player.setYRot(yaw);
        client.player.setXRot(pitch);
    }

    private static void startSmokeTrail(Vec3 target) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        Vec3 eyePos = client.player.getEyePosition();
        if (eyePos.distanceToSqr(target) < SMOKE_TRAIL_MIN_DISTANCE_SQUARED) {
            return;
        }

        smokeTrailStart = eyePos;
        smokeTrailTarget = target;
        smokeTrailTicks = SMOKE_TRAIL_TICKS;
        smokeTrailProgress = 0.0;
    }

    private static void tickSmokeTrail(Minecraft client) {
        if (smokeTrailTicks <= 0 || client.level == null) {
            if (smokeTrailTicks <= 0) {
                smokeTrailProgress = 0.0;
            }
            return;
        }

        double nextProgress = smoothProgress((SMOKE_TRAIL_TICKS - smokeTrailTicks + 1) / (double) SMOKE_TRAIL_TICKS);
        Vec3 delta = smokeTrailTarget.subtract(smokeTrailStart);
        for (int i = 0; i < SMOKE_PARTICLES_PER_TICK; i++) {
            double particleProgress = smokeTrailProgress
                + (nextProgress - smokeTrailProgress) * (i / (double) Math.max(1, SMOKE_PARTICLES_PER_TICK - 1));
            Vec3 position = smokeTrailStart.add(delta.scale(particleProgress));
            LocateTrailParticle particle = EasyInventoryCrafterConfig.getLocateTrailParticle();
            double jitter = particle == LocateTrailParticle.END_ROD ? 0.03 : 0.08;
            double verticalVelocity = particle == LocateTrailParticle.END_ROD ? 0.0 : 0.01;
            double jitterX = (client.level.getRandom().nextDouble() - 0.5) * jitter;
            double jitterY = (client.level.getRandom().nextDouble() - 0.5) * jitter;
            double jitterZ = (client.level.getRandom().nextDouble() - 0.5) * jitter;
            client.particleEngine.createParticle(
                getTrailParticleEffect(particle),
                position.x + jitterX,
                position.y + jitterY,
                position.z + jitterZ,
                0.0,
                verticalVelocity,
                0.0
            );
        }

        smokeTrailProgress = nextProgress;
        smokeTrailTicks--;
        if (smokeTrailTicks <= 0) {
            smokeTrailProgress = 0.0;
        }
    }

    private static double smoothProgress(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static ParticleOptions getTrailParticleEffect(LocateTrailParticle particle) {
        return switch (particle) {
            case WATER_EVAPORATION -> ParticleTypes.CLOUD;
            case SMOKE -> ParticleTypes.SMOKE;
            case END_ROD -> ParticleTypes.END_ROD;
        };
    }


    public static void resetAutoRefreshCounter() {
        autoRefreshCounter = 0;
    }

    public static void incrementAutoRefreshCounter() {
        autoRefreshCounter++;
    }

    public static boolean shouldAutoRefresh(int intervalTicks) {
        return intervalTicks > 0 && autoRefreshCounter >= intervalTicks;
    }
}
