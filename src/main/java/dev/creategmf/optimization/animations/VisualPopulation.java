package dev.creategmf.optimization.animations;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
/** Live AbstractBlockEntityVisual population. Construction/deletion only; snapshots are on-demand. */
public final class VisualPopulation {
    private static final Set<VisualAnimationState> STATES = ConcurrentHashMap.newKeySet();
    private VisualPopulation() {}
    public static void add(VisualAnimationState state) { STATES.add(state); }
    public static void remove(VisualAnimationState state) { STATES.remove(state); }
    public static void cleanWorld() { STATES.removeIf(state -> !state.knownWorldOwner()); }
    public static Snapshot snapshot() {
        int visible = 0, offScreen = 0, addons = 0, occluded = 0;
        for (VisualAnimationState state : STATES) {
            if (!state.knownWorldOwner()) continue;
            if (OffScreenAnimations.visibleOrUnknown(state.worldBounds())) visible++; else offScreen++;
            if (dev.creategmf.optimization.occlusion.EntityCullingBridge.isCulled(state.owner)) occluded++;
            if (!"create".equals(state.descriptor.namespace())) addons++;
        }
        return new Snapshot(visible, offScreen, addons, occluded);
    }
    public record Snapshot(int visible, int offScreen, int addons, int occluded) {
        public int total() { return visible + offScreen; }
    }
}
