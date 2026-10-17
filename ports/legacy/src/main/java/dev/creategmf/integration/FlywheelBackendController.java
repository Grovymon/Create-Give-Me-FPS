package dev.creategmf.integration;

import com.jozufozu.flywheel.config.BackendType;
import com.jozufozu.flywheel.config.FlwConfig;
import dev.creategmf.CreateGmf;
import dev.creategmf.config.FlywheelBackendMode;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;

public final class FlywheelBackendController {
    private FlywheelBackendController() {}
    public static boolean apply(FlywheelBackendMode mode) {
        try {
            CreateOcclusionController.clearForRendererChange();
            BackendType backend = switch (mode) {
                case OFF -> BackendType.OFF;
                case INDIRECT -> BackendType.BATCHING;
                case DEFAULT, INSTANCING -> BackendType.INSTANCING;
            };
            FlwConfig.get().client.backend.set(backend);
            FlwConfig.get().client.backend.save();
            return true;
        } catch (LinkageError | RuntimeException error) {
            CreateGmf.LOGGER.warn("Could not persist Flywheel 0.6 backend", error);
            return false;
        }
    }
}
