package dev.creategmf.benchmark;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class OptimizationTargetTest {
    @Test void restartExperimentsNeverRunLive() {
        assertFalse(OptimizationTarget.ACCELERATED_RENDERER.runtimeSafe());
        assertFalse(OptimizationTarget.FLYWHEEL_BACKEND.runtimeSafe());
        assertTrue(OptimizationTarget.OFFSCREEN.runtimeSafe());
        assertTrue(OptimizationTarget.PARTICLES.runtimeSafe());
    }
}
