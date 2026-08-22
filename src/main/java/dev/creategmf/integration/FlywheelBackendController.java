package dev.creategmf.integration;

import dev.creategmf.CreateGmf;
import dev.creategmf.config.FlywheelBackendMode;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;
import dev.engine_room.flywheel.impl.NeoForgeFlwConfig;

/**
 * Persists only Flywheel's own supported backend identifiers.
 *
 * Flywheel owns visual managers used by Create. Replacing that backend while a
 * level is already open can leave a partial visual manager behind (notably on
 * steam engines), so the value intentionally takes effect on the next game
 * start instead of attempting an unsafe live reload.
 */
public final class FlywheelBackendController {
    private FlywheelBackendController() {
    }

    public static boolean apply(FlywheelBackendMode mode) {
        try {
            // Visual managers are recreated only after restart.  Restore any
            // GMF-hidden visual before persisting the pending backend change.
            CreateOcclusionController.clearForRendererChange();
            NeoForgeFlwConfig.INSTANCE.client.backend.set(mode.flywheelValue());
            NeoForgeFlwConfig.INSTANCE.client.backend.save();

            return true;
        } catch (LinkageError | RuntimeException exception) {
            CreateGmf.LOGGER.warn("Could not switch Flywheel backend to {}", mode.flywheelValue(), exception);
            return false;
        }
    }
}
