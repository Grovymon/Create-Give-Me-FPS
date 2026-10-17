package dev.creategmf.optimization.animations;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import com.simibubi.create.content.kinetics.base.flwdata.BeltData;
import com.simibubi.create.content.kinetics.base.flwdata.KineticData;
import com.simibubi.create.foundation.utility.AnimationTickHolder;
import dev.creategmf.legacy.KineticDataAccess;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Freezes Flywheel 0.6 shader time without changing Create's kinetic state. */
public final class RotationAnimationRegistry {
    private static final ThreadLocal<java.util.Deque<BlockEntity>> CONSTRUCTION_OWNER = ThreadLocal.withInitial(java.util.ArrayDeque::new);
    private static final Map<KineticData, Entry> ENTRIES = new WeakHashMap<>();
    private static long bucket = Long.MIN_VALUE;
    private RotationAnimationRegistry() {}
    public static void beginConstruction(BlockEntity owner) { CONSTRUCTION_OWNER.get().push(owner); }
    public static void endConstruction() {
        var stack = CONSTRUCTION_OWNER.get();
        stack.pop();
        if (stack.isEmpty()) CONSTRUCTION_OWNER.remove();
    }
    public static void record(KineticData data, Float speed, Float offset) {
        synchronized (ENTRIES) {
            Entry entry = ENTRIES.get(data);
            if (entry == null) {
                BlockEntity owner = CONSTRUCTION_OWNER.get().peek();
                if (owner == null || !owner.getClass().getName().startsWith("com.simibubi.create.")) return;
                var access = (KineticDataAccess) data;
                entry = new Entry(owner.getBlockPos().immutable(), owner.getClass().getName(),
                        access.createGmf$getSpeed(), access.createGmf$getOffset());
                ENTRIES.put(data, entry);
            }
            if (speed != null) entry.speed = speed;
            if (offset != null) entry.offset = offset;
        }
    }
    public static void beforeUpload(KineticData data) {
        Entry entry;
        synchronized (ENTRIES) { entry = ENTRIES.get(data); }
        if (entry != null) apply(data, entry, false);
    }
    public static void onClientTick() { snapshot().forEach(e -> apply(e.data(), e.entry(), false)); }
    public static void onFrame() {
        long current = DistantAnimationController.currentAnimationBucket();
        if (current == bucket) return;
        bucket = current;
        snapshot().forEach(e -> apply(e.data(), e.entry(), true));
    }
    private static List<Tracked> snapshot() {
        synchronized (ENTRIES) {
            List<Tracked> out = new ArrayList<>(ENTRIES.size());
            ENTRIES.forEach((data, entry) -> out.add(new Tracked(data, entry)));
            return out;
        }
    }
    private static void apply(KineticData data, Entry entry, boolean sample) {
        Minecraft mc = Minecraft.getInstance();
        var camera = mc.gameRenderer == null ? null : mc.gameRenderer.getMainCamera();
        AnimationPolicy policy = DistantAnimationController.animationPolicy(entry.position, entry.owner, camera);
        if (policy != entry.policy || (policy == AnimationPolicy.REDUCED && sample)) {
            float ticks = mc.level == null ? 0 : AnimationTickHolder.getRenderTime(mc.level);
            // Verified against Create 0.5.1.i's rotating.vert and belt.vert.
            float factor = data instanceof BeltData ? 1f / (31.5f * 16f) : 3f / 10f;
            entry.sampledOffset = entry.offset + ticks * entry.speed * factor;
        }
        entry.policy = policy;
        float speed = policy == AnimationPolicy.FULL ? entry.speed : 0;
        float offset = policy == AnimationPolicy.FULL ? entry.offset : entry.sampledOffset;
        var access = (KineticDataAccess) data;
        if (access.createGmf$getSpeed() != speed || access.createGmf$getOffset() != offset) {
            access.createGmf$setSpeed(speed);
            access.createGmf$setOffset(offset);
            data.markDirty();
        }
    }
    private static final class Entry {
        final BlockPos position;
        final String owner;
        float speed, offset, sampledOffset;
        AnimationPolicy policy;
        Entry(BlockPos position, String owner, float speed, float offset) {
            this.position = position; this.owner = owner; this.speed = speed; this.offset = offset;
        }
    }
    private record Tracked(KineticData data, Entry entry) {}
}
