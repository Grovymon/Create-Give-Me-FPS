package dev.creategmf.optimization.animations;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.phys.AABB;

/** Camera frustum only. No raycasts, Entity Culling calls or visual queue operations. */
public final class OffScreenAnimations {
    private static volatile Frustum frustum;
    private OffScreenAnimations() {}
    public static void prepare(Frustum value) { frustum = value; }
    public static void clear() { frustum = null; }
    public static boolean visibleOrUnknown(AABB bounds) {
        Frustum current = frustum;
        if (current == null || bounds == null || !finite(bounds)) return true;
        return current.isVisible(bounds);
    }
    private static boolean finite(AABB b) {
        return Double.isFinite(b.minX) && Double.isFinite(b.minY) && Double.isFinite(b.minZ)
                && Double.isFinite(b.maxX) && Double.isFinite(b.maxY) && Double.isFinite(b.maxZ);
    }
}
