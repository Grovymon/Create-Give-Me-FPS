package dev.creategmf.optimization.occlusion;

import java.util.IdentityHashMap;
import java.util.Map;

import dev.creategmf.config.GmfConfig;
import dev.creategmf.integration.ModCompatibilityDetector;
import dev.engine_room.flywheel.api.visualization.VisualManager;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Client-only visual hand-off adapted from Create: Nowheel.
 *
 * <p>Copyright (c) 2026 Lap2ka, MIT License. See
 * {@code META-INF/THIRD_PARTY_NOTICES.md} in the distributed JAR.</p>
 *
 * <p>Entity Culling decides whether an object is hidden. GMF only mirrors the
 * resulting transition into Flywheel's block-entity visual queue; it never
 * raycasts, changes a kinetic network, or changes a block entity tick.</p>
 */
public final class CreateOcclusionController {
    private static final Map<BlockEntity, Boolean> TRACKED_CULLED = new IdentityHashMap<>();
    private static ClientLevel activeLevel;
    private static int visible;
    private static int occluded;
    private static long visualRemovals;
    private static long visualRestores;
    private static long stateChanges;
    private static long creationSkips;

    private CreateOcclusionController() {
    }

    /** Returns true only when this mod, not Nowheel, owns the visual queue. */
    public static boolean isActive() {
        return GmfConfig.SPEC.isLoaded()
                && GmfConfig.CLIENT.enabled.get()
                && GmfConfig.CLIENT.createOcclusionCulling.get()
                && EntityCullingBridge.isAvailable()
                && !ModCompatibilityDetector.hasNowheel();
    }

    public static boolean shouldRejectVisualCreation(BlockEntity blockEntity) {
        if (!isActive() || !isCreateBlockEntity(blockEntity) || !EntityCullingBridge.isCulled(blockEntity)) {
            return false;
        }
        track(blockEntity, true);
        creationSkips++;
        return true;
    }

    /**
     * Makes Flywheel-only Create visuals visible to Entity Culling's scan.
     *
     * <p>Many Create block entities intentionally have no vanilla
     * {@code BlockEntityRenderer}: Flywheel owns their visual entirely. Entity
     * Culling normally skips such objects before it can set their cull state.
     * The optional CullTask mixin supplies a no-op renderer only for this
     * visibility test; it never renders a second copy of the mechanism.</p>
     */
    public static boolean shouldExposeToEntityCulling(BlockEntity blockEntity) {
        return isActive() && isCreateBlockEntity(blockEntity);
    }

    /** Called only by Entity Culling's own state transition hook. */
    public static void onCullChanged(BlockEntity blockEntity, boolean nowCulled) {
        if (!isActive() || !isCreateBlockEntity(blockEntity)) return;
        Boolean previous = TRACKED_CULLED.get(blockEntity);
        boolean changed = track(blockEntity, nowCulled);
        if (!changed) return;
        // The first visible observation only establishes state. Queueing an add
        // for every currently loaded mechanism would duplicate Flywheel work.
        if (previous == null && !nowCulled) return;
        VisualManager<BlockEntity> visuals = visualManager(blockEntity.getLevel());
        if (visuals == null) return;
        if (nowCulled) {
            visuals.queueRemove(blockEntity);
            visualRemovals++;
        } else {
            visuals.queueAdd(blockEntity);
            visualRestores++;
        }
    }

    public static void forget(BlockEntity blockEntity) {
        Boolean previous = TRACKED_CULLED.remove(blockEntity);
        if (previous == null) return;
        if (previous) occluded = Math.max(0, occluded - 1);
        else visible = Math.max(0, visible - 1);
    }

    /** Restores any hidden visuals before backend replacement or disabling. */
    public static void clearForRendererChange() {
        restoreTrackedVisuals();
    }

    public static void onConfigurationChanged() {
        if (!isActive()) {
            restoreTrackedVisuals();
        } else {
            refreshLoadedCreateBlockEntities();
        }
    }

    public static void onClientTick() {
        ClientLevel current = Minecraft.getInstance().level;
        if (current == activeLevel) return;
        // A previous level's manager may already be gone at this point. Do not
        // issue queue calls into it; merely discard references on world change.
        TRACKED_CULLED.clear();
        visible = 0;
        occluded = 0;
        activeLevel = current;
    }

    public static Snapshot snapshot() {
        String provider = ModCompatibilityDetector.hasNowheel() ? "nowheel"
                : EntityCullingBridge.isAvailable() ? "entityculling" : "none";
        return new Snapshot(isActive(), EntityCullingBridge.isAvailable(), ModCompatibilityDetector.hasNowheel(), provider,
                TRACKED_CULLED.size(), visible, occluded, visualRemovals, visualRestores, stateChanges, creationSkips);
    }

    private static boolean track(BlockEntity blockEntity, boolean nowCulled) {
        Boolean old = TRACKED_CULLED.put(blockEntity, nowCulled);
        if (old == null) {
            if (nowCulled) occluded++;
            else visible++;
            return true;
        }
        if (old == nowCulled) return false;
        if (nowCulled) {
            visible = Math.max(0, visible - 1);
            occluded++;
        } else {
            occluded = Math.max(0, occluded - 1);
            visible++;
        }
        stateChanges++;
        return true;
    }

    private static void restoreTrackedVisuals() {
        for (Map.Entry<BlockEntity, Boolean> entry : TRACKED_CULLED.entrySet()) {
            if (!entry.getValue()) continue;
            VisualManager<BlockEntity> visuals = visualManager(entry.getKey().getLevel());
            if (visuals != null) {
                visuals.queueAdd(entry.getKey());
                visualRestores++;
            }
        }
        TRACKED_CULLED.clear();
        visible = 0;
        occluded = 0;
    }

    private static void refreshLoadedCreateBlockEntities() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) return;
        int radius = Math.max(2, minecraft.options.getEffectiveRenderDistance());
        int centerX = minecraft.player.chunkPosition().x;
        int centerZ = minecraft.player.chunkPosition().z;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (BlockEntity blockEntity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (!isCreateBlockEntity(blockEntity)) continue;
                    onCullChanged(blockEntity, EntityCullingBridge.isCulled(blockEntity));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static VisualManager<BlockEntity> visualManager(Level level) {
        if (level == null) return null;
        VisualizationManager manager = VisualizationManager.get(level);
        return manager == null ? null : (VisualManager<BlockEntity>) manager.blockEntities();
    }

    private static boolean isCreateBlockEntity(BlockEntity blockEntity) {
        return blockEntity != null && blockEntity.getClass().getName().startsWith("com.simibubi.create.")
                && VisualizationHelper.canVisualize(blockEntity);
    }

    public record Snapshot(boolean active, boolean entityCullingAvailable, boolean nowheelDetected, String provider,
            int tracked, int visible, int occluded, long visualRemovals, long visualRestores, long stateChanges,
            long creationSkips) {
    }
}
