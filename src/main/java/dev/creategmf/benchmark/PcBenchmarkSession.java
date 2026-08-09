package dev.creategmf.benchmark;

import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.PcProfile;
import dev.creategmf.diagnostics.EvidenceStatus;
import dev.creategmf.profiler.FrameStatistics;
import dev.creategmf.profiler.MemoryMetricsCollector;
import dev.creategmf.profiler.MemorySnapshot;
import dev.creategmf.util.LongRingBuffer;

public final class PcBenchmarkSession {
    public static final PcBenchmarkSession INSTANCE = new PcBenchmarkSession();

    public enum State {
        IDLE,
        WARMUP,
        MEASURING,
        COMPLETE
    }

    private static final long WARMUP_NANOS = 2_000_000_000L;
    private static final long MEASURE_NANOS = 5_000_000_000L;
    private static final long SLICE_BUDGET_NANOS = 1_500_000L;

    private final LongRingBuffer frames = new LongRingBuffer(4096);
    private State state = State.IDLE;
    private long phaseStartNanos;
    private long measuredWorkNanos;
    private long measuredOperations;
    private MemorySnapshot memoryBefore = MemorySnapshot.EMPTY;
    private BenchmarkResult result;
    private volatile double blackhole;

    private PcBenchmarkSession() {
    }

    public void start() {
        frames.clear();
        measuredWorkNanos = 0;
        measuredOperations = 0;
        result = null;
        phaseStartNanos = System.nanoTime();
        state = State.WARMUP;
    }

    public void cancel() {
        if (state == State.WARMUP || state == State.MEASURING) {
            state = State.IDLE;
            frames.clear();
        }
    }

    public void tick() {
        if (state == State.IDLE || state == State.COMPLETE) {
            return;
        }
        long workStart = System.nanoTime();
        long operations = runWorkSlice(workStart + SLICE_BUDGET_NANOS);
        long elapsed = System.nanoTime() - workStart;
        long now = System.nanoTime();

        if (state == State.WARMUP) {
            if (now - phaseStartNanos >= WARMUP_NANOS) {
                MemoryMetricsCollector.INSTANCE.sample();
                memoryBefore = MemoryMetricsCollector.INSTANCE.snapshot();
                frames.clear();
                phaseStartNanos = now;
                state = State.MEASURING;
            }
            return;
        }

        measuredOperations += operations;
        measuredWorkNanos += elapsed;
        if (now - phaseStartNanos >= MEASURE_NANOS) {
            finish();
        }
    }

    public void onFrame(long frameNanos) {
        if (state == State.MEASURING && frameNanos > 0) {
            frames.add(frameNanos);
        }
    }

    private long runWorkSlice(long deadline) {
        long operations = 0;
        double x = blackhole + 0.125;
        int index = 1;
        while (System.nanoTime() < deadline) {
            x = Math.fma(x, 1.0000001192092896, index * 0.000001);
            x -= Math.floor(x * 0.0001) * 0.00001;
            index = (index * 1664525 + 1013904223) & 0x7fffffff;
            operations += 8;
        }
        blackhole = x;
        return operations;
    }

    private void finish() {
        MemoryMetricsCollector.INSTANCE.sample();
        MemorySnapshot memoryAfter = MemoryMetricsCollector.INSTANCE.snapshot();
        FrameStatistics statistics = FrameStatistics.fromNanos(frames.snapshot());
        double operationsPerMillisecond = measuredWorkNanos == 0 ? 0
                : measuredOperations / (measuredWorkNanos / 1_000_000.0);
        PcProfile profile = selectProfile(operationsPerMillisecond, statistics, memoryAfter);
        result = new BenchmarkResult(operationsPerMillisecond, statistics, memoryBefore, memoryAfter, profile,
                EvidenceStatus.ESTIMATED);
        GmfConfig.CLIENT.pcProfile.set(profile);
        GmfConfig.save();
        state = State.COMPLETE;
    }

    private static PcProfile selectProfile(double operationsPerMillisecond, FrameStatistics frames,
            MemorySnapshot memory) {
        int logical = Runtime.getRuntime().availableProcessors();
        double memoryGiB = memory.heapMaxBytes() / 1_073_741_824.0;
        int points = logical >= 16 ? 3 : logical >= 8 ? 2 : logical >= 4 ? 1 : 0;
        points += memoryGiB >= 6 ? 2 : memoryGiB >= 3 ? 1 : 0;
        points += operationsPerMillisecond >= 80_000 ? 2 : operationsPerMillisecond >= 35_000 ? 1 : 0;
        if (frames.samples() >= 60 && frames.averageFps() >= GmfConfig.CLIENT.targetFps.get()) {
            points++;
        }
        return points >= 7 ? PcProfile.ULTRA : points >= 5 ? PcProfile.HIGH
                : points >= 3 ? PcProfile.MEDIUM : PcProfile.LOW;
    }

    public double progress() {
        long elapsed = System.nanoTime() - phaseStartNanos;
        return switch (state) {
            case WARMUP -> Math.min(0.25, 0.25 * elapsed / WARMUP_NANOS);
            case MEASURING -> Math.min(1.0, 0.25 + 0.75 * elapsed / MEASURE_NANOS);
            case COMPLETE -> 1.0;
            default -> 0.0;
        };
    }

    public State state() {
        return state;
    }

    public BenchmarkResult result() {
        return result;
    }
}
