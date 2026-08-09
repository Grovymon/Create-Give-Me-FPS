package dev.creategmf.optimization.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import com.simibubi.create.content.processing.burner.ScrollInstance;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

/** Freezes or samples shader-time scrolling used by belts and conveyors. */
public final class ScrollAnimationRegistry {
    private static final Map<ScrollInstance, Entry> ENTRIES = new WeakHashMap<>();
    private static long lastSampleBucket = Long.MIN_VALUE;

    private ScrollAnimationRegistry() {
    }

    public static void prepare(ScrollInstance instance, BlockPos position, MechanismAnimationGroup group) {
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.computeIfAbsent(instance, ignored -> new Entry());
            entry.position = position;
            entry.group = group;
        }
    }

    /** @return true when this instance has an explicit mechanism owner. */
    public static boolean beforeUpload(ScrollInstance instance) {
        Entry entry;
        synchronized (ENTRIES) {
            entry = ENTRIES.get(instance);
        }
        if (entry == null) {
            return false;
        }

        if (instance.speedU != 0 || instance.speedV != 0) {
            entry.originalSpeedU = instance.speedU;
            entry.originalSpeedV = instance.speedV;
            entry.baseOffsetU = instance.offsetU;
            entry.baseOffsetV = instance.offsetV;
        }
        apply(instance, entry, false);
        return true;
    }

    public static void onClientTick() {
        snapshot().forEach(tracked -> apply(tracked.instance(), tracked.entry(), false));
    }

    public static void onFrame() {
        long bucket = DistantAnimationController.currentAnimationBucket();
        if (bucket == lastSampleBucket) {
            return;
        }
        lastSampleBucket = bucket;
        snapshot().forEach(tracked -> {
            if (policy(tracked.entry()) == AnimationPolicy.REDUCED) {
                apply(tracked.instance(), tracked.entry(), true);
            }
        });
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
        return DistantAnimationController.animationPolicy(entry.position == null ? null
                : net.minecraft.world.phys.Vec3.atCenterOf(entry.position), entry.group, camera);
    }

    private static List<Tracked> snapshot() {
        synchronized (ENTRIES) {
            List<Tracked> tracked = new ArrayList<>(ENTRIES.size());
            ENTRIES.forEach((instance, entry) -> tracked.add(new Tracked(instance, entry)));
            return tracked;
        }
    }

    private static final class Entry {
        private BlockPos position;
        private MechanismAnimationGroup group;
        private float originalSpeedU;
        private float originalSpeedV;
        private float baseOffsetU;
        private float baseOffsetV;
        private float sampledOffsetU;
        private float sampledOffsetV;
        private AnimationPolicy lastPolicy;
    }

    private record Tracked(ScrollInstance instance, Entry entry) {
    }
}
