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
    private static ClientLevel animationLevel;

    private DistantAnimationController() {
    }

    public static void nextFrame() {
        frameIndex++;
    }

    public static long frameIndex() { return frameIndex; }

    public static boolean shouldUpdate(Object visual, Camera camera) {
        GmfRuntimeStatus.markAnimationHook();
        if (!(visual instanceof BlockEntityVisualAccess access)) return true;
        VisualAnimationState state = access.createGmf$getAnimationState();
        AnimationPolicy policy = animationPolicy(state, camera);
        long bucket = currentAnimationBucket();
        boolean update = policy == AnimationPolicy.FULL
                || policy == AnimationPolicy.REDUCED && state.lastBucket != bucket;
        if (update) state.lastBucket = bucket;
        boolean outside = state.knownWorldOwner() && !OffScreenAnimations.visibleOrUnknown(state.worldBounds());
        if (state.knownWorldOwner()) VisualWorkCounters.observe(outside, !update, state.wasPaused && update,
                !"create".equals(state.descriptor.namespace()));
        state.wasPaused = !update;
        return updateScrollableVisual(visual, update);
    }

    public static boolean shouldTick(Object visual) {
        if (!GmfConfig.CLIENT.enabled.get() || !(visual instanceof BlockEntityVisualAccess access)) return true;
        VisualAnimationState state = access.createGmf$getAnimationState();
        if (!state.knownWorldOwner()) return true;
        AnimationPolicy policy = animationPolicy(state, Minecraft.getInstance().gameRenderer.getMainCamera());
        return policy != AnimationPolicy.STATIC && Math.floorMod(tickIndex + state.owner.getBlockPos().asLong(),
                GmfConfig.CLIENT.animationUpdateTickDivisor.get()) == 0
                && (policy == AnimationPolicy.FULL || shouldUpdateReducedTick(state.owner.getBlockPos().hashCode()));
    }

    public static AnimationPolicy animationPolicy(VisualAnimationState state, Camera camera) {
        if (!state.knownWorldOwner() || camera == null) return AnimationPolicy.FULL;
        boolean outside = GmfConfig.CLIENT.pauseOffScreenAnimations.get()
                && state.grace.paused(frameIndex, OffScreenAnimations.visibleOrUnknown(state.worldBounds()));
        BlockPos pos = state.owner.getBlockPos();
        return AnimationRules.evaluate(GmfConfig.CLIENT.enabled.get(), true,
                animationsEnabled(state.descriptor.group())
                        && !GmfConfig.CLIENT.disabledAddonMechanisms.get().contains(state.descriptor.registryId()),
                GmfConfig.CLIENT.distantAnimationDistance.get(),
                camera.getPosition().distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5),
                GmfConfig.CLIENT.distantAnimationMode.get(), outside);
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
        boolean known = position != null && camera != null;
        return AnimationRules.evaluate(GmfConfig.CLIENT.enabled.get(), known, animationsEnabled(group),
                GmfConfig.CLIENT.distantAnimationDistance.get(),
                known ? camera.getPosition().distanceToSqr(position) : Double.NaN,
                GmfConfig.CLIENT.distantAnimationMode.get(), false);
    }

    public static long currentAnimationBucket() {
        long intervalNanos = 1_000_000_000L / Math.max(1, GmfConfig.CLIENT.reducedAnimationFps.get());
        return Math.floorDiv(System.nanoTime(), intervalNanos);
    }

    public static boolean shouldUpdateReducedTick(int visualId) {
        int interval = Math.max(1, (int) Math.ceil(20.0 / Math.max(1, GmfConfig.CLIENT.reducedAnimationFps.get())));
        return Math.floorMod(tickIndex + visualId, interval) == 0;
    }

    public static void tick() {
        tickIndex++;
        ClientLevel currentLevel = Minecraft.getInstance().level;
        if (animationLevel != currentLevel) {
            RotationAnimationRegistry.cleanWorld();
            ScrollAnimationRegistry.cleanWorld();
            VisualPopulation.cleanWorld();
            OffScreenAnimations.clear();
            animationLevel = currentLevel;
        }
        RotationAnimationRegistry.onClientTick();
        ScrollAnimationRegistry.onClientTick();
        // Movement does not rebuild visuals. Registered instances resume from captured source values.
    }

    public static void refreshLoadedKinetics() {
        Minecraft minecraft = Minecraft.getInstance();
        int radius = Math.max(2, minecraft.options.getEffectiveRenderDistance());
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
                    if (dev.creategmf.integration.CreateVisualClassifier.classify(blockEntity).supported()) {
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

    public static boolean shouldSuppressUnpositioned(MechanismAnimationGroup group) {
        return GmfConfig.CLIENT.enabled.get()
                && (GmfConfig.CLIENT.distantAnimationDistance.get() <= 0 || !animationsEnabled(group));
    }

    private static boolean updateScrollableVisual(Object visual, boolean shouldUpdate) {
        if (visual instanceof ScrollableVisualAccess access) {
            access.createGmf$setAnimationSuppressed(!shouldUpdate);
        }
        return shouldUpdate;
    }
}
