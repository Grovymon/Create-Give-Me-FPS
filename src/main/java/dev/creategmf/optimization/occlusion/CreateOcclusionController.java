package dev.creategmf.optimization.occlusion;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.integration.CreateVisualClassifier;
import dev.creategmf.integration.ModCompatibilityDetector;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client hand-off adapted from Create: Nowheel, Copyright (c) 2026 Lap2ka (MIT).
 * EC worker callbacks only publish observations; client thread owns queues and cleanup.
 * Unknown visibility is visible. Never changes world/kinetic/server state.
 */
public final class CreateOcclusionController {
    private static final Map<BlockEntity, HiddenVisualState> TRACKED = new IdentityHashMap<>();
    private static final Set<BlockEntity> PENDING = ConcurrentHashMap.newKeySet();
    private static final Set<BlockEntity> REJECTED = ConcurrentHashMap.newKeySet();
    private static final Set<BlockEntity> FORGOTTEN = ConcurrentHashMap.newKeySet();
    private static final LongAdder creationSkips = new LongAdder();
    private static ClientLevel activeLevel;
    private static VisualizationManager activeManager;
    private static boolean wasActive;
    private static long removals, restores, changes;
    private static int visible, hidden, sweepTicks;
    private CreateOcclusionController() {}
    public static boolean isActive() {
        return GmfConfig.SPEC.isLoaded() && GmfConfig.CLIENT.enabled.get()
                && GmfConfig.CLIENT.createOcclusionCulling.get() && EntityCullingBridge.isAvailable()
                && !ModCompatibilityDetector.hasNowheel();
    }
    public static boolean shouldRejectVisualCreation(BlockEntity be) {
        return shouldRejectVisualCreation(be, true);
    }
    public static boolean shouldRejectVisualCreation(BlockEntity be, boolean absent) {
        if (!isActive() || !compatible(be) || !EntityCullingBridge.isCulled(be)) return false;
        if (absent) REJECTED.add(be);
        PENDING.add(be); creationSkips.increment();
        return true;
    }
    public static boolean shouldExposeToEntityCulling(BlockEntity be) { return isActive() && compatible(be); }
    public static void onCullChanged(BlockEntity be, boolean ignoredWorkerValue) {
        if (isActive() && compatible(be)) PENDING.add(be);
    }
    public static void forget(BlockEntity be) {
        if (be.getLevel() != null && be.getLevel().isClientSide) FORGOTTEN.add(be);
    }
    public static void clearForRendererChange() { restoreTrackedVisuals(); activeManager = null; }
    public static void onConfigurationChanged() {
        if (!isActive()) restoreTrackedVisuals(); else seedLoaded();
    }
    public static void onClientTick() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current != activeLevel) {
            TRACKED.clear(); activeLevel = current; activeManager = null; wasActive = false;
            visible = hidden = 0;
            PENDING.removeIf(be -> be.getLevel() != current);
            REJECTED.removeIf(be -> be.getLevel() != current);
            FORGOTTEN.clear();
        }
        VisualizationManager manager = current == null ? null : VisualizationManager.get(current);
        if (manager != activeManager) {
            // A disposed manager must not receive restoration commands.
            TRACKED.clear(); visible = hidden = 0; activeManager = manager;
            if (manager != null && isActive()) seedLoaded();
        }
        boolean active = isActive();
        if (!active && wasActive) restoreTrackedVisuals();
        if (active && !wasActive) seedLoaded();
        wasActive = active;
        for (BlockEntity be : FORGOTTEN) {
            TRACKED.remove(be); PENDING.remove(be); REJECTED.remove(be); FORGOTTEN.remove(be);
        }
        TRACKED.keySet().removeIf(be -> be.isRemoved() || be.getLevel() != activeLevel);
        if (manager == null) return;
        int budget = 1024;
        for (BlockEntity be : PENDING) {
            if (--budget < 0) break;
            PENDING.remove(be);
            if (be.isRemoved() || be.getLevel() != activeLevel) { REJECTED.remove(be); continue; }
            HiddenVisualState state = TRACKED.computeIfAbsent(be, ignored -> new HiddenVisualState());
            if (REJECTED.remove(be)) state.creationRejected();
            apply(be, state, active, EntityCullingBridge.isCulled(be));
        }
        // Forced-visible timeouts need not invoke setCulled. Reconcile ownership at 5 Hz.
        if (++sweepTicks >= 4) {
            sweepTicks = 0;
            TRACKED.forEach((be, state) -> apply(be, state, active, EntityCullingBridge.isCulled(be)));
        }
        visible = hidden = 0;
        for (HiddenVisualState state : TRACKED.values()) {
            if (state.owesRestore()) hidden++; else visible++;
        }
    }
    private static void apply(BlockEntity be, HiddenVisualState state, boolean active, boolean culled) {
        if (activeManager == null || be.isRemoved() || be.getLevel() != activeLevel) return;
        switch (state.transition(active, culled)) {
            case REMOVE -> { activeManager.blockEntities().queueRemove(be); removals++; changes++; }
            case ADD -> { activeManager.blockEntities().queueAdd(be); restores++; changes++; }
            case NONE -> {}
        }
    }
    private static void restoreTrackedVisuals() {
        if (activeManager == null) return; // Keep restoration debt until a queue is available.
        TRACKED.forEach((be, state) -> apply(be, state, false, false));
        for (BlockEntity be : REJECTED) {
            if (be.getLevel() == activeLevel && !be.isRemoved() && !TRACKED.containsKey(be)) {
                activeManager.blockEntities().queueAdd(be); restores++;
            }
        }
        TRACKED.clear(); REJECTED.clear(); PENDING.clear(); visible = hidden = 0;
    }
    private static void seedLoaded() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        int radius = mc.options.getEffectiveRenderDistance();
        int cx = mc.player.chunkPosition().x, cz = mc.player.chunkPosition().z;
        for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
            if (!mc.level.hasChunk(x, z)) continue;
            for (BlockEntity be : mc.level.getChunk(x, z).getBlockEntities().values()) if (compatible(be)) PENDING.add(be);
        }
    }
    private static boolean compatible(BlockEntity be) {
        return be != null && be.getLevel() != null && be.getLevel().isClientSide
                && CreateVisualClassifier.classify(be).supported() && VisualizationHelper.canVisualize(be);
    }
    public static Snapshot snapshot() {
        String provider = ModCompatibilityDetector.hasNowheel() ? "nowheel"
                : EntityCullingBridge.isAvailable() ? "entityculling" : "none";
        return new Snapshot(isActive(), EntityCullingBridge.isAvailable(), ModCompatibilityDetector.hasNowheel(),
                provider, TRACKED.size(), visible, hidden, removals, restores, changes, creationSkips.sum());
    }
    public record Snapshot(boolean active, boolean entityCullingAvailable, boolean nowheelDetected, String provider,
            int tracked, int visible, int occluded, long visualRemovals, long visualRestores, long stateChanges,
            long creationSkips) {}
}
