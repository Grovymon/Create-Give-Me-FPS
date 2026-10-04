package dev.creategmf.optimization.animations;

import java.util.concurrent.atomic.LongAdder;
/** Observed update opportunities, not GPU time or unique visual population. */
public final class VisualWorkCounters {
    private static final LongAdder visible = new LongAdder(), offScreen = new LongAdder(),
            skipped = new LongAdder(), restored = new LongAdder(), addon = new LongAdder();
    private VisualWorkCounters() {}
    public static void observe(boolean outside, boolean skip, boolean resume, boolean isAddon) {
        if (outside) offScreen.increment(); else visible.increment();
        if (skip) skipped.increment();
        if (resume) restored.increment();
        if (isAddon) addon.increment();
    }
    public static Snapshot snapshot() {
        return new Snapshot(visible.sum(), offScreen.sum(), skipped.sum(), restored.sum(), addon.sum());
    }
    public record Snapshot(long visibleUpdates, long offScreenUpdates, long skippedUpdates,
            long restoredUpdates, long addonUpdates) {
        public Snapshot subtract(Snapshot start) {
            return new Snapshot(visibleUpdates - start.visibleUpdates, offScreenUpdates - start.offScreenUpdates,
                    skippedUpdates - start.skippedUpdates, restoredUpdates - start.restoredUpdates,
                    addonUpdates - start.addonUpdates);
        }
    }
}
