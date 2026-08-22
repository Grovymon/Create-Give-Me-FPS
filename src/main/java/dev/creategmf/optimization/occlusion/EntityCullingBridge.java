package dev.creategmf.optimization.occlusion;

import java.lang.reflect.Method;

import dev.creategmf.CreateGmf;
import dev.creategmf.integration.ModCompatibilityDetector;

/**
 * Optional, reflection-only bridge to Entity Culling's {@code Cullable} API.
 *
 * <p>No Entity Culling type appears in a descriptor or mixin target. This is
 * important: GMF must still load normally when that optional mod is absent or
 * changes its package. A failed lookup simply leaves every visual visible.</p>
 */
public final class EntityCullingBridge {
    private static boolean resolved;
    private static boolean available;
    private static Class<?> cullableType;
    private static Method isCulled;
    private static Method isForcedVisible;

    private EntityCullingBridge() {
    }

    public static boolean isAvailable() {
        resolve();
        return available;
    }

    public static boolean isCulled(Object subject) {
        if (subject == null || !isAvailable() || !cullableType.isInstance(subject)) return false;
        try {
            return Boolean.TRUE.equals(isCulled.invoke(subject))
                    && !Boolean.TRUE.equals(isForcedVisible.invoke(subject));
        } catch (ReflectiveOperationException | LinkageError error) {
            disable(error);
            return false;
        }
    }

    private static void resolve() {
        if (resolved) return;
        resolved = true;
        if (!ModCompatibilityDetector.hasEntityCulling()) return;
        try {
            ClassLoader loader = EntityCullingBridge.class.getClassLoader();
            cullableType = Class.forName("dev.tr7zw.entityculling.versionless.access.Cullable", false, loader);
            isCulled = cullableType.getMethod("isCulled");
            isForcedVisible = cullableType.getMethod("isForcedVisible");
            available = true;
        } catch (ReflectiveOperationException | LinkageError error) {
            disable(error);
        }
    }

    private static void disable(Throwable error) {
        if (available || ModCompatibilityDetector.hasEntityCulling()) {
            CreateGmf.LOGGER.warn("Entity Culling bridge is unavailable; GMF will not hide Create visuals.", error);
        }
        available = false;
        cullableType = null;
        isCulled = null;
        isForcedVisible = null;
    }
}
