package dev.creategmf.optimization.animations;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.simibubi.create.content.processing.burner.ScrollInstance;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Freezes or samples shader-time scrolling used by belts and conveyors. */
public final class ScrollAnimationRegistry {
    private static final Map<ScrollInstance, Entry> ENTRIES = new ConcurrentHashMap<>();
    private static long lastSampleBucket = Long.MIN_VALUE;

    private ScrollAnimationRegistry() {
    }

    public static void prepare(ScrollInstance instance, VisualAnimationState state) {
        ENTRIES.computeIfAbsent(instance, ignored -> new Entry(state));
    }
    /** Parameters come directly from Create's setters, including real zero speed, never rendered fields. */
    public static void sourceSpeed(ScrollInstance instance, float u, float v) {
        Entry entry = ENTRIES.get(instance);
        if (entry == null) return;
        entry.originalSpeedU = u;
        entry.originalSpeedV = v;
    }
    public static void sourceOffset(ScrollInstance instance, float u, float v) {
        Entry entry = ENTRIES.get(instance);
        if (entry == null) return;
        entry.baseOffsetU = u;
        entry.baseOffsetV = v;
    }
    public static void forget(ScrollInstance instance) { ENTRIES.remove(instance); }
    public static void clear() { ENTRIES.clear(); }
    public static void cleanWorld() { ENTRIES.entrySet().removeIf(entry -> !entry.getValue().state.knownWorldOwner()); }

    /** @return true when this instance has an explicit mechanism owner. */
    public static boolean beforeUpload(ScrollInstance instance) {
        Entry entry = ENTRIES.get(instance);
        if (entry == null) {
            return false;
        }

        apply(instance, entry, false);
        return true;
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

    private static void apply(ScrollInstance instance, Entry entry, boolean sampleReduced) {
        AnimationPolicy policy = policy(entry);
        boolean changed = policy != entry.lastPolicy;

        if (policy == AnimationPolicy.FULL) {
            changed |= instance.speedU != entry.originalSpeedU || instance.speedV != entry.originalSpeedV
                    || instance.offsetU != entry.baseOffsetU || instance.offsetV != entry.baseOffsetV;
            instance.speedU = entry.originalSpeedU;
            instance.speedV = entry.originalSpeedV;
            instance.offsetU = entry.baseOffsetU;
            instance.offsetV = entry.baseOffsetV;
        } else {
            if (entry.lastPolicy != policy || (policy == AnimationPolicy.REDUCED && sampleReduced)) {
                sample(entry);
                changed = true;
            }
            changed |= instance.speedU != 0 || instance.speedV != 0
                    || instance.offsetU != entry.sampledOffsetU || instance.offsetV != entry.sampledOffsetV;
            instance.speedU = 0;
            instance.speedV = 0;
            instance.offsetU = entry.sampledOffsetU;
            instance.offsetV = entry.sampledOffsetV;
        }

        entry.lastPolicy = policy;
        if (changed) {
            instance.handle().setChanged();
        }
    }

    private static void sample(Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        float ticks = minecraft.level == null ? 0 : AnimationTickHolder.getRenderTime(minecraft.level);
        entry.sampledOffsetU = entry.baseOffsetU + ticks * entry.originalSpeedU;
        entry.sampledOffsetV = entry.baseOffsetV + ticks * entry.originalSpeedV;
    }

    private static AnimationPolicy policy(Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer == null ? null : minecraft.gameRenderer.getMainCamera();
        return DistantAnimationController.animationPolicy(entry.state, camera);
    }

    private static final class Entry {
        private final VisualAnimationState state;
        private Entry(VisualAnimationState state) { this.state = state; }
        private volatile float originalSpeedU;
        private volatile float originalSpeedV;
        private volatile float baseOffsetU;
        private volatile float baseOffsetV;
        private float sampledOffsetU;
        private float sampledOffsetV;
        private AnimationPolicy lastPolicy;
    }

}
