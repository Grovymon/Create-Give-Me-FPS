package dev.creategmf.optimization.animations;

import dev.creategmf.config.DistantAnimationMode;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.diagnostics.GmfRuntimeStatus;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;

public final class DistantAnimationController {
    public enum AnimationPolicy {
        FULL,
        REDUCED,
        STATIC
    }

    private static long frameIndex;
    private static long tickIndex;
    private static Vec3 lastRefreshPosition;

    private DistantAnimationController() {
    }

    public static void nextFrame() {
        frameIndex++;
        RotationAnimationRegistry.onFrame();
        ScrollAnimationRegistry.onFrame();
    }

    public static boolean shouldUpdate(Object visual, Camera camera) {
        GmfRuntimeStatus.markAnimationHook();
        if (!GmfConfig.CLIENT.enabled.get() || !(visual instanceof BlockEntityVisualAccess access)
                || !visual.getClass().getName().startsWith("com.simibubi.create.")) {
            return true;
        }
        MechanismAnimationGroup group = MechanismAnimationGroup.fromClassName(visual.getClass().getName());
        if (!animationsEnabled(group)) {
            return updateScrollableVisual(visual, false);
        }
        BlockPos pos = access.createGmf$getWorldPosition();
        double limit = GmfConfig.CLIENT.distantAnimationDistance.get();
        if (limit <= 0) {
            return updateScrollableVisual(visual, false);
        }
        double distanceSquared = camera.getPosition().distanceToSqr(Vec3.atCenterOf(pos));
        if (distanceSquared <= limit * limit) {
            return updateScrollableVisual(visual, true);
        }
        DistantAnimationMode mode = GmfConfig.CLIENT.distantAnimationMode.get();
        boolean shouldUpdate = switch (mode) {
            case FULL -> true;
            case REDUCED -> distanceSquared <= limit * limit * 4
                    && Math.floorMod(frameIndex + pos.asLong(), 6) == 0;
            case STATIC -> false;
        };
        return updateScrollableVisual(visual, shouldUpdate);
    }

    public static boolean shouldTick(Object visual) {
        if (!GmfConfig.CLIENT.enabled.get() || !(visual instanceof BlockEntityVisualAccess access)
                || !visual.getClass().getName().startsWith("com.simibubi.create.")) {
            return true;
        }
        MechanismAnimationGroup group = MechanismAnimationGroup.fromClassName(visual.getClass().getName());
        if (!animationsEnabled(group)) {
            return false;
        }
        double limit = GmfConfig.CLIENT.distantAnimationDistance.get();
        if (limit <= 0) {
            return false;
        }
        // This is a client visual update divider only.  Create's kinetic
        // network, stress calculation and machine processing stay untouched.
        if (Math.floorMod(tickIndex + access.createGmf$getWorldPosition().asLong(),
                GmfConfig.CLIENT.animationUpdateTickDivisor.get()) != 0) {
            return false;
        }
        DistantAnimationMode mode = GmfConfig.CLIENT.distantAnimationMode.get();
        if (mode == DistantAnimationMode.FULL) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return true;
        }
        BlockPos pos = access.createGmf$getWorldPosition();
        double distanceSquared = minecraft.gameRenderer.getMainCamera().getPosition()
                .distanceToSqr(Vec3.atCenterOf(pos));
        if (distanceSquared <= limit * limit) {
            return true;
        }
        return mode == DistantAnimationMode.REDUCED
                && distanceSquared <= limit * limit * 4
                && Math.floorMod(tickIndex + pos.asLong(), 3) == 0;
    }

    public static boolean shouldBeStatic(BlockPos pos, String ownerClassName) {
        return animationPolicy(pos, ownerClassName, Minecraft.getInstance().gameRenderer.getMainCamera())
                == AnimationPolicy.STATIC;
    }

    public static AnimationPolicy animationPolicy(BlockPos pos, String ownerClassName, Camera camera) {
        return animationPolicy(pos == null ? null : Vec3.atCenterOf(pos),
                MechanismAnimationGroup.fromClassName(ownerClassName), camera);
    }

    public static AnimationPolicy animationPolicy(BlockPos pos, MechanismAnimationGroup group, Camera camera) {
        return animationPolicy(pos == null ? null : Vec3.atCenterOf(pos), group, camera);
    }

    public static AnimationPolicy animationPolicy(Vec3 position, MechanismAnimationGroup group, Camera camera) {
        if (!GmfConfig.CLIENT.enabled.get()) {
            return AnimationPolicy.FULL;
        }
        if (!animationsEnabled(group) || GmfConfig.CLIENT.distantAnimationDistance.get() <= 0) {
            return AnimationPolicy.STATIC;
        }
        DistantAnimationMode mode = GmfConfig.CLIENT.distantAnimationMode.get();
        if (mode == DistantAnimationMode.FULL) {
            return AnimationPolicy.FULL;
        }
        if (position == null || camera == null) {
            return mode == DistantAnimationMode.REDUCED ? AnimationPolicy.REDUCED : AnimationPolicy.STATIC;
        }
        double limit = GmfConfig.CLIENT.distantAnimationDistance.get();
        double distanceSquared = camera.getPosition().distanceToSqr(position);
        if (distanceSquared <= limit * limit) {
            return AnimationPolicy.FULL;
        }
        if (mode == DistantAnimationMode.REDUCED && distanceSquared <= limit * limit * 4) {
            return AnimationPolicy.REDUCED;
        }
        return AnimationPolicy.STATIC;
    }

    public static long currentAnimationBucket() {
        long intervalNanos = 1_000_000_000L / Math.max(1, GmfConfig.CLIENT.reducedAnimationFps.get());
        return Math.floorDiv(System.nanoTime(), intervalNanos);
    }

    public static void tick() {
        tickIndex++;
        RotationAnimationRegistry.onClientTick();
        ScrollAnimationRegistry.onClientTick();
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || GmfConfig.CLIENT.distantAnimationMode.get() == DistantAnimationMode.FULL) {
            lastRefreshPosition = null;
            return;
        }
        Vec3 current = minecraft.player.position();
        if (lastRefreshPosition == null) {
            lastRefreshPosition = current;
            refreshLoadedKinetics();
        } else if (current.distanceToSqr(lastRefreshPosition) >= 4096) {
            lastRefreshPosition = current;
            refreshAnimationBoundary();
        }
    }

    public static void refreshLoadedKinetics() {
        Minecraft minecraft = Minecraft.getInstance();
        int radius = Math.max(2, minecraft.options.getEffectiveRenderDistance());
        refreshCreateVisuals(minecraft, radius);
    }

    private static void refreshAnimationBoundary() {
        Minecraft minecraft = Minecraft.getInstance();
        int configuredRadius = (int) Math.ceil(GmfConfig.CLIENT.distantAnimationDistance.get() / 16.0) + 2;
        int radius = Math.min(Math.max(2, minecraft.options.getEffectiveRenderDistance()), configuredRadius);
        refreshCreateVisuals(minecraft, radius);
    }

    private static void refreshCreateVisuals(Minecraft minecraft, int radius) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        int centerX = minecraft.player.chunkPosition().x;
        int centerZ = minecraft.player.chunkPosition().z;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (!level.hasChunk(x, z)) {
                    continue;
                }
                for (BlockEntity blockEntity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (blockEntity.getClass().getName().startsWith("com.simibubi.create.")) {
                        VisualizationHelper.queueUpdate(blockEntity);
                    }
                }
            }
        }
    }

    public static boolean animationsEnabled(MechanismAnimationGroup group) {
        return GmfConfig.CLIENT.mechanismAnimations.get(group).get();
    }

    public static float sampledRenderTime(float original, BlockPos position, MechanismAnimationGroup group) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer == null ? null : minecraft.gameRenderer.getMainCamera();
        AnimationPolicy policy = animationPolicy(position == null ? null : Vec3.atCenterOf(position), group, camera);
        if (policy == AnimationPolicy.FULL) {
            return original;
        }
        if (policy == AnimationPolicy.STATIC) {
            return 0;
        }
        float stepTicks = 20f / Math.max(1, GmfConfig.CLIENT.reducedAnimationFps.get());
        return (float) Math.floor(original / stepTicks) * stepTicks;
    }

    public static boolean animationsGloballyDisabled() {
        // Sprite tickers can run while resources are loading, before the
        // NeoForge config has been attached.  Treat that short period as normal
        // rendering instead of reading an unavailable ConfigValue.
        return GmfConfig.SPEC.isLoaded()
                && GmfConfig.CLIENT.enabled.get()
                && GmfConfig.CLIENT.distantAnimationDistance.get() <= 0;
    }

    public static boolean shouldSuppressUnpositioned(MechanismAnimationGroup group) {
        return GmfConfig.CLIENT.enabled.get()
                && (GmfConfig.CLIENT.distantAnimationDistance.get() <= 0 || !animationsEnabled(group));
    }

    public static boolean shouldFreezeSharedFluidTextures() {
        return animationsGloballyDisabled();
    }

    public static boolean shouldFreezeUnclassifiedScrolling(double x, double y, double z) {
        if (!GmfConfig.CLIENT.enabled.get()) {
            return false;
        }
        if (GmfConfig.CLIENT.distantAnimationDistance.get() <= 0 || allMechanismAnimationsDisabled()) {
            return true;
        }
        if (GmfConfig.CLIENT.distantAnimationMode.get() == DistantAnimationMode.FULL) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        double limit = GmfConfig.CLIENT.distantAnimationDistance.get();
        return minecraft.gameRenderer.getMainCamera().getPosition().distanceToSqr(x, y, z) > limit * limit;
    }

    private static boolean allMechanismAnimationsDisabled() {
        return GmfConfig.CLIENT.mechanismAnimations.values().stream().noneMatch(value -> value.get());
    }

    private static boolean updateScrollableVisual(Object visual, boolean shouldUpdate) {
        if (visual instanceof ScrollableVisualAccess access) {
            access.createGmf$setAnimationSuppressed(!shouldUpdate);
        }
        return shouldUpdate;
    }
}
