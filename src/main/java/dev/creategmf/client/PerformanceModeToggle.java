package dev.creategmf.client;

import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.PcProfile;
import dev.creategmf.config.VisualConfigSnapshot;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;

/** Session-only temporary Potato; snapshot survives dimension changes, but never leaks to the next launch. */
public final class PerformanceModeToggle {
    private static VisualConfigSnapshot previous;
    private PerformanceModeToggle() {}
    public static void toggle() {
        if (previous == null) {
            previous = VisualConfigSnapshot.capture();
            GmfConfig.applyProfile(PcProfile.POTATO);
        } else {
            previous.restore();
            previous = null;
        }
        DistantAnimationController.refreshLoadedKinetics();
        CreateOcclusionController.onConfigurationChanged();
    }
    public static void restoreOnDisconnect() {
        if (previous != null) { previous.restore(); previous = null; }
    }
    public static void saveWithoutTemporaryOverrides(Runnable save) {
        if (previous == null) { save.run(); return; }
        var temporary = VisualConfigSnapshot.capture();
        try { previous.restore(); save.run(); } finally { temporary.restore(); }
    }
}
