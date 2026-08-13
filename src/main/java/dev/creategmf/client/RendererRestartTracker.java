package dev.creategmf.client;

import dev.creategmf.config.FlywheelBackendMode;
import dev.creategmf.config.GmfConfig;

/**
 * Remembers the renderer choices with which the current game session started.
 * Returning both values to that state clears the restart requirement again.
 */
public final class RendererRestartTracker {
    private static RendererState launchState;

    private RendererRestartTracker() {
    }

    public static void captureLaunchState() {
        launchState = currentState();
    }

    public static boolean isRestartRequired() {
        if (launchState == null) {
            captureLaunchState();
        }
        return !launchState.equals(currentState());
    }

    private static RendererState currentState() {
        return new RendererState(
                GmfConfig.CLIENT.flywheelBackend.get(),
                GmfConfig.CLIENT.acceleratedRenderer.get());
    }

    private record RendererState(FlywheelBackendMode backend, boolean acceleratedRenderer) {
    }
}
