package dev.creategmf.optimization.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
    private static final Map<RotatingInstance, Entry> ENTRIES = new WeakHashMap<>();
    private static long lastSampleBucket = Long.MIN_VALUE;

    private RotationAnimationRegistry() {
    }

    public static void register(RotatingInstance instance, BlockPos position, String ownerClassName) {
        Entry entry;
        synchronized (ENTRIES) {
            entry = ENTRIES.computeIfAbsent(instance, ignored -> new Entry());
            entry.position = position;
            entry.ownerClassName = ownerClassName;
            entry.originalSpeed = instance.rotationalSpeed;
            entry.baseOffset = instance.rotationOffset;
        }
        apply(instance, entry, true);
    }

    public static void beforeUpload(RotatingInstance instance) {
        Entry entry;
        synchronized (ENTRIES) {
            entry = ENTRIES.get(instance);
        }
        if (entry == null) {
            return;
        }

        AnimationPolicy policy = policy(entry);
        if (instance.rotationalSpeed != 0) {
            entry.originalSpeed = instance.rotationalSpeed;
            entry.baseOffset = instance.rotationOffset;
        } else if (policy == AnimationPolicy.FULL) {
            entry.originalSpeed = 0;
            entry.baseOffset = instance.rotationOffset;
        }
        apply(instance, entry, false);
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

    /**
     * WeakHashMap may expunge collected keys during an ordinary read. Keeping a
     * strong per-pass snapshot also prevents Flywheel uploads from structurally
     * changing the registry while animation policies are being applied.
     */
    private static List<Tracked> snapshot() {
        synchronized (ENTRIES) {
            List<Tracked> tracked = new ArrayList<>(ENTRIES.size());
            ENTRIES.forEach((instance, entry) -> tracked.add(new Tracked(instance, entry)));
            return tracked;
        }
    }

    private static void apply(RotatingInstance instance, Entry entry, boolean sampleReduced) {
        AnimationPolicy policy = policy(entry);
        boolean changed = policy != entry.lastPolicy;

        if (policy == AnimationPolicy.FULL) {
            changed |= instance.rotationalSpeed != entry.originalSpeed || instance.rotationOffset != entry.baseOffset;
            instance.rotationalSpeed = entry.originalSpeed;
            instance.rotationOffset = entry.baseOffset;
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
        return DistantAnimationController.animationPolicy(entry.position, entry.ownerClassName, camera);
    }

    private static float sampledOffset(Entry entry) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return entry.baseOffset;
        }
        float renderSeconds = AnimationTickHolder.getRenderTime(minecraft.level) / 20f;
        return entry.baseOffset + renderSeconds * entry.originalSpeed;
    }

    private static final class Entry {
        private BlockPos position;
        private String ownerClassName;
        private float originalSpeed;
        private float baseOffset;
        private float sampledOffset;
        private AnimationPolicy lastPolicy;
    }

    private record Tracked(RotatingInstance instance, Entry entry) {
    }
}
