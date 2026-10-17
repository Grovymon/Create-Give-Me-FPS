package dev.creategmf.optimization.occlusion;

import java.lang.reflect.Method;
import dev.creategmf.CreateGmf;
import dev.creategmf.integration.ModCompatibilityDetector;

/** Guarded reflection, no copied Entity Culling implementation. Unknown means visible. */
public final class EntityCullingBridge {
    private record Binding(Class<?> type, Method culled, Method forcedVisible) {}
    private static volatile boolean resolved;
    private static volatile Binding binding;
    private EntityCullingBridge() {}
    public static boolean isAvailable() {
        if (!resolved) resolve();
        return binding != null;
    }
    public static boolean isCulled(Object subject) {
        if (!resolved) resolve();
        Binding current = binding;
        if (subject == null || current == null || !current.type().isInstance(subject)) return false;
        try {
            return Boolean.TRUE.equals(current.culled().invoke(subject))
                    && !Boolean.TRUE.equals(current.forcedVisible().invoke(subject));
        } catch (ReflectiveOperationException | LinkageError | IllegalArgumentException error) {
            disable(error);
            return false;
        }
    }
    private static synchronized void resolve() {
        if (resolved) return;
        if (ModCompatibilityDetector.hasEntityCulling()) {
            Throwable failure = null;
            for (String name : new String[] {
                    "dev.tr7zw.entityculling.access.Cullable",
                    "dev.tr7zw.entityculling.versionless.access.Cullable" }) {
                try {
                    Class<?> type = Class.forName(name, false, EntityCullingBridge.class.getClassLoader());
                    binding = new Binding(type, type.getMethod("isCulled"), type.getMethod("isForcedVisible"));
                    break;
                } catch (ReflectiveOperationException | LinkageError error) { failure = error; }
            }
            if (binding == null) CreateGmf.LOGGER.warn("Entity Culling API unavailable; leaving visuals visible.", failure);
        }
        resolved = true;
    }
    private static synchronized void disable(Throwable error) {
        if (binding != null) {
            binding = null;
            CreateGmf.LOGGER.warn("Entity Culling API failed; leaving visuals visible.", error);
        }
    }
}
