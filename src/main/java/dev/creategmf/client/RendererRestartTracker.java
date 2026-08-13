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
        if (GmfConfig.SPEC.isLoaded()) {
            launchState = currentState();
        }
    }

    public static boolean isRestartRequired() {
        // A mixin can create client-side objects before NeoForge finishes loading
        // the client config.  Reading ConfigValue#get at that stage crashes startup.
        if (!GmfConfig.SPEC.isLoaded()) {
            return false;
        }
        if (launchState == null) {
            captureLaunchState();
            return false;
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
