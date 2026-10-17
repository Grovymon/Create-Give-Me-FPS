package dev.creategmf.integration;

import dev.creategmf.CreateGmf;
import dev.creategmf.config.FlywheelBackendMode;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;
import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.BackendManager;
import dev.engine_room.flywheel.impl.FabricFlwConfig;
import net.minecraft.resources.ResourceLocation;

public final class FlywheelBackendController {
    private FlywheelBackendController() {}
    public static boolean apply(FlywheelBackendMode mode) {
        try {
            CreateOcclusionController.clearForRendererChange();
            // Save a separate config object, leaving this launch's live backend
            // untouched until restart. Retain all other Flywheel options.
            FabricFlwConfig pending = new FabricFlwConfig(FabricFlwConfig.PATH.toFile());
            pending.load();
            pending.useDefaultBackend = mode == FlywheelBackendMode.DEFAULT;
            pending.backend = pending.useDefaultBackend ? BackendManager.offBackend()
                    : Backend.REGISTRY.getOrThrow(new ResourceLocation(mode.flywheelValue()));
            pending.save();
            return true;
        } catch (LinkageError | RuntimeException error) {
            CreateGmf.LOGGER.warn("Could not persist Flywheel backend {}", mode.flywheelValue(), error);
            return false;
        }
    }
}
