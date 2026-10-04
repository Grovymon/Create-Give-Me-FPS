package dev.creategmf.benchmark;

import dev.creategmf.optimization.belts.BeltShadowCounters;
import dev.creategmf.optimization.belts.BeltShadowOptimizer;
import dev.creategmf.profiler.FrameStatistics;
import dev.creategmf.profiler.ServerTickCollector;
import dev.creategmf.util.LongRingBuffer;

public final class OptimizationBenchmarkSession {
    public static final OptimizationBenchmarkSession INSTANCE = new OptimizationBenchmarkSession();

    public enum State {
        IDLE,
        BEFORE,
        TRANSITION,
        AFTER,
        COMPLETE
    }

    private static final long PHASE_NANOS = 5_000_000_000L;
    private static final long TRANSITION_NANOS = 1_000_000_000L;

    private final LongRingBuffer beforeFrames = new LongRingBuffer(4096);
    private final LongRingBuffer afterFrames = new LongRingBuffer(4096);
    private State state = State.IDLE;
    private long phaseStart;
    private BeltShadowCounters counterStart;
    private BeltShadowCounters beforeCounters;
    private ServerTickCollector.Totals tickStart;
    private double beforeMspt;
    private OptimizationBenchmarkResult result;
    private OptimizationTarget target = OptimizationTarget.BELT_SHADOWS;
    private dev.creategmf.config.VisualConfigSnapshot previous;
    private dev.creategmf.diagnostics.RenderEvidence evidenceStart, beforeEvidence;
    private dev.creategmf.profiler.MemorySnapshot memoryStart, beforeMemory;
    private net.minecraft.client.multiplayer.ClientLevel level;
    private dev.creategmf.config.CreateParticleMode particleMode;
    private dev.creategmf.config.DistantAnimationMode distantMode;

    private OptimizationBenchmarkSession() {
    }

    public void start() {
        start(OptimizationTarget.BELT_SHADOWS);
    }
    public boolean start(OptimizationTarget chosen) {
        if (!chosen.runtimeSafe() || net.minecraft.client.Minecraft.getInstance().level == null) return false;
        cancel();
        target = chosen;
        level = net.minecraft.client.Minecraft.getInstance().level;
        previous = dev.creategmf.config.VisualConfigSnapshot.capture();
        particleMode = dev.creategmf.config.GmfConfig.CLIENT.createParticleMode.get();
        distantMode = dev.creategmf.config.GmfConfig.CLIENT.distantAnimationMode.get();
        beforeFrames.clear();
        afterFrames.clear();
        result = null;
        counterStart = BeltShadowOptimizer.counters();
        tickStart = ServerTickCollector.totals();
        try { phase(false); beginEvidence(); }
        catch (RuntimeException error) { cancel(); throw error; }
        phaseStart = System.nanoTime();
        state = State.BEFORE;
        return true;
    }

    public void cancel() {
        restore();
        state = State.IDLE;
    }

    public void onFrame(long frameNanos) {
        if (frameNanos <= 0) {
            return;
        }
        if (state == State.BEFORE) {
            beforeFrames.add(frameNanos);
        } else if (state == State.AFTER) {
            afterFrames.add(frameNanos);
        }
    }

    public void tick() {
        try { advance(); }
        catch (RuntimeException error) { cancel(); dev.creategmf.CreateGmf.LOGGER.warn("GMF A/B cancelled safely", error); }
    }
    private void advance() {
        if (!isActive()) return;
        if (net.minecraft.client.Minecraft.getInstance().level != level) { cancel(); return; }
        long elapsed = System.nanoTime() - phaseStart;
        if (state == State.BEFORE && elapsed >= PHASE_NANOS) {
            beforeCounters = BeltShadowOptimizer.counters().subtract(counterStart);
            beforeMspt = ServerTickCollector.averageBetween(tickStart, ServerTickCollector.totals());
            dev.creategmf.profiler.MemoryMetricsCollector.INSTANCE.sample();
            beforeMemory = dev.creategmf.profiler.MemoryMetricsCollector.INSTANCE.snapshot();
            beforeEvidence = dev.creategmf.diagnostics.RenderEvidence.capture().since(evidenceStart);
            phase(true);
            phaseStart = System.nanoTime();
            state = State.TRANSITION;
        } else if (state == State.TRANSITION && elapsed >= TRANSITION_NANOS) {
            counterStart = BeltShadowOptimizer.counters();
            tickStart = ServerTickCollector.totals();
            beginEvidence();
            phaseStart = System.nanoTime();
            state = State.AFTER;
        } else if (state == State.AFTER && elapsed >= PHASE_NANOS) {
            BeltShadowCounters afterCounters = BeltShadowOptimizer.counters().subtract(counterStart);
            double afterMspt = ServerTickCollector.averageBetween(tickStart, ServerTickCollector.totals());
            dev.creategmf.profiler.MemoryMetricsCollector.INSTANCE.sample();
            var afterMemory = dev.creategmf.profiler.MemoryMetricsCollector.INSTANCE.snapshot();
            var afterEvidence = dev.creategmf.diagnostics.RenderEvidence.capture().since(evidenceStart);
            result = new OptimizationBenchmarkResult(
                    FrameStatistics.fromNanos(beforeFrames.snapshot()),
                    FrameStatistics.fromNanos(afterFrames.snapshot()),
                    beforeMspt,
                    afterMspt,
                    beforeCounters,
                    afterCounters, target, beforeEvidence, afterEvidence,
                    Math.max(0, beforeMemory.gcCount() - beforeGcStart),
                    Math.max(0, afterMemory.gcCount() - memoryStart.gcCount()),
                    Math.max(0, beforeMemory.gcCollectionMilliseconds() - beforeGcMsStart),
                    Math.max(0, afterMemory.gcCollectionMilliseconds() - memoryStart.gcCollectionMilliseconds()));
            restore();
            state = State.COMPLETE;
        }
    }
    private long beforeGcStart, beforeGcMsStart;
    private void beginEvidence() {
        evidenceStart = dev.creategmf.diagnostics.RenderEvidence.capture();
        dev.creategmf.profiler.MemoryMetricsCollector.INSTANCE.sample();
        memoryStart = dev.creategmf.profiler.MemoryMetricsCollector.INSTANCE.snapshot();
        if (state == State.IDLE) {
            beforeGcStart = memoryStart.gcCount();
            beforeGcMsStart = memoryStart.gcCollectionMilliseconds();
        }
    }
    private void phase(boolean optimized) {
        var c = dev.creategmf.config.GmfConfig.CLIENT;
        // Restore all unrelated settings between phases, then modify ONLY the selected visual feature.
        previous.restore();
        c.enabled.set(true);
        switch (target) {
            case BELT_SHADOWS -> BeltShadowOptimizer.forceEnabled(optimized);
            case OCCLUSION -> c.createOcclusionCulling.set(optimized);
            case OFFSCREEN -> c.pauseOffScreenAnimations.set(optimized);
            case DISTANT -> {
                if (!optimized && c.distantAnimationDistance.get() <= 0) c.distantAnimationDistance.set(32.0);
                c.distantAnimationMode.set(!optimized ? dev.creategmf.config.DistantAnimationMode.FULL
                        : distantMode == dev.creategmf.config.DistantAnimationMode.FULL
                        ? dev.creategmf.config.DistantAnimationMode.REDUCED : distantMode);
            }
            case TRANSPORTED_ITEMS -> c.renderTransportedBeltItems.set(!optimized);
            case PARTICLES -> {
                c.createParticleMode.set(!optimized ? dev.creategmf.config.CreateParticleMode.FULL
                        : particleMode == dev.creategmf.config.CreateParticleMode.FULL
                        ? dev.creategmf.config.CreateParticleMode.REDUCED : particleMode);
                if (!optimized) {
                    c.renderSteamSmokeParticles.set(true); c.renderSparkParticles.set(true);
                    c.renderItemBreakParticles.set(true); c.renderWaterSplashParticles.set(true);
                }
            }
            default -> throw new IllegalStateException("Restart-required targets cannot run live");
        }
        dev.creategmf.optimization.occlusion.CreateOcclusionController.onConfigurationChanged();
    }
    private void restore() {
        BeltShadowOptimizer.forceEnabled(null);
        if (previous != null) {
            previous.restore();
            previous = null;
            dev.creategmf.optimization.occlusion.CreateOcclusionController.onConfigurationChanged();
        }
    }

    public double progress() {
        long elapsed = System.nanoTime() - phaseStart;
        return switch (state) {
            case BEFORE -> Math.min(0.45, 0.45 * elapsed / PHASE_NANOS);
            case TRANSITION -> Math.min(0.55, 0.45 + 0.10 * elapsed / TRANSITION_NANOS);
            case AFTER -> Math.min(1.0, 0.55 + 0.45 * elapsed / PHASE_NANOS);
            case COMPLETE -> 1.0;
            default -> 0.0;
        };
    }

    public State state() {
        return state;
    }

    public OptimizationBenchmarkResult result() {
        return result;
    }

    public boolean isActive() {
        return state == State.BEFORE || state == State.TRANSITION || state == State.AFTER;
    }
}
