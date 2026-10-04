package dev.creategmf.optimization.animations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.content.kinetics.base.RotatingInstance;

import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/**
 * Converts Flywheel's shader-time rotation into sampled rotation when GMF is
 * reducing or disabling an animation. Merely skipping visual update plans is
 * insufficient because the rotating shader advances from global render time.
 */
public final class RotationAnimationRegistry {
    private static final Map<RotatingInstance, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static long lastSampleBucket = Long.MIN_VALUE;

    private RotationAnimationRegistry() {
    }

    public static void register(RotatingInstance instance, net.minecraft.world.level.block.entity.BlockEntity owner) {
        Entry entry = ENTRIES.computeIfAbsent(instance, ignored -> new Entry(new VisualAnimationState(owner)));
        apply(instance, entry, true);
    }
    public static void prepare(RotatingInstance instance, net.minecraft.world.level.block.entity.BlockEntity owner) {
        ENTRIES.computeIfAbsent(instance, ignored -> new Entry(new VisualAnimationState(owner)));
    }

    public static void sourceSpeed(RotatingInstance instance, float value) {
        Entry entry = ENTRIES.get(instance);
        if (entry != null) entry.source.speed(value); // Includes a real zero RPM update.
    }
    public static void sourceOffset(RotatingInstance instance, float value) {
        Entry entry = ENTRIES.get(instance);
        if (entry != null) entry.source.offset(value);
    }
    public static void forget(RotatingInstance instance) { ENTRIES.remove(instance); }
    public static void clear() { ENTRIES.clear(); }
    public static void cleanWorld() { ENTRIES.entrySet().removeIf(entry -> !entry.getValue().state.knownWorldOwner()); }

    public static void beforeUpload(RotatingInstance instance) {
        Entry entry = ENTRIES.get(instance);
        if (entry == null) {
            return;
        }

        // Do not infer source speed from a value previously zeroed by GMF.
        apply(instance, entry, false);
    }

    public static void onClientTick() {
        ENTRIES.forEach((instance, entry) -> apply(instance, entry, false));
    }

    public static void onFrame() {
        long bucket = DistantAnimationController.currentAnimationBucket();
        boolean sample = bucket != lastSampleBucket;
        lastSampleBucket = bucket;
        ENTRIES.forEach((instance, entry) -> apply(instance, entry, sample));
    }

    private static void apply(RotatingInstance instance, Entry entry, boolean sampleReduced) {
        AnimationPolicy policy = policy(entry);
        boolean changed = policy != entry.lastPolicy;

        if (policy == AnimationPolicy.FULL) {
            changed |= instance.rotationalSpeed != entry.source.speed() || instance.rotationOffset != entry.source.offset();
            instance.rotationalSpeed = entry.source.renderedSpeed(false);
            instance.rotationOffset = entry.source.offset();
        } else if (policy == AnimationPolicy.REDUCED) {
            if (sampleReduced || entry.lastPolicy != AnimationPolicy.REDUCED) {
                entry.sampledOffset = sampledOffset(entry);
                changed = true;
            }
            changed |= instance.rotationalSpeed != 0 || instance.rotationOffset != entry.sampledOffset;
            instance.rotationalSpeed = 0;
            instance.rotationOffset = entry.sampledOffset;
        } else {
            if (entry.lastPolicy != AnimationPolicy.STATIC) {
                entry.sampledOffset = sampledOffset(entry);
                changed = true;
            }
            changed |= instance.rotationalSpeed != 0 || instance.rotationOffset != entry.sampledOffset;
            instance.rotationalSpeed = 0;
            instance.rotationOffset = entry.sampledOffset;
        }

        entry.lastPolicy = policy;
        if (changed) {
            instance.handle().setChanged();
        }
    }

    private static AnimationPolicy policy(Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer == null ? null : minecraft.gameRenderer.getMainCamera();
        return DistantAnimationController.animationPolicy(entry.state, camera);
    }

    private static float sampledOffset(Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return entry.source.offset();
        }
        float renderSeconds = AnimationTickHolder.getRenderTime(minecraft.level) / 20f;
        return entry.source.offset() + renderSeconds * entry.source.speed();
    }

    private static final class Entry {
        private final VisualAnimationState state;
        private Entry(VisualAnimationState state) { this.state = state; }
        private final ShaderAnimationSource source = new ShaderAnimationSource();
        private float sampledOffset;
        private AnimationPolicy lastPolicy;
    }

}
