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

    private OptimizationBenchmarkSession() {
    }

    public void start() {
        beforeFrames.clear();
        afterFrames.clear();
        result = null;
        counterStart = BeltShadowOptimizer.counters();
        tickStart = ServerTickCollector.totals();
        BeltShadowOptimizer.forceEnabled(false);
        phaseStart = System.nanoTime();
        state = State.BEFORE;
    }

    public void cancel() {
        BeltShadowOptimizer.forceEnabled(null);
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
        long elapsed = System.nanoTime() - phaseStart;
        if (state == State.BEFORE && elapsed >= PHASE_NANOS) {
            beforeCounters = BeltShadowOptimizer.counters().subtract(counterStart);
            beforeMspt = ServerTickCollector.averageBetween(tickStart, ServerTickCollector.totals());
            BeltShadowOptimizer.forceEnabled(true);
            phaseStart = System.nanoTime();
            state = State.TRANSITION;
        } else if (state == State.TRANSITION && elapsed >= TRANSITION_NANOS) {
            counterStart = BeltShadowOptimizer.counters();
            tickStart = ServerTickCollector.totals();
            phaseStart = System.nanoTime();
            state = State.AFTER;
        } else if (state == State.AFTER && elapsed >= PHASE_NANOS) {
            BeltShadowCounters afterCounters = BeltShadowOptimizer.counters().subtract(counterStart);
            double afterMspt = ServerTickCollector.averageBetween(tickStart, ServerTickCollector.totals());
            BeltShadowOptimizer.forceEnabled(null);
            result = new OptimizationBenchmarkResult(
                    FrameStatistics.fromNanos(beforeFrames.snapshot()),
                    FrameStatistics.fromNanos(afterFrames.snapshot()),
                    beforeMspt,
                    afterMspt,
                    beforeCounters,
                    afterCounters);
            state = State.COMPLETE;
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
