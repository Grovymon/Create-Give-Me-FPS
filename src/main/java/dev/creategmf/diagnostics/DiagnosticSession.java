package dev.creategmf.diagnostics;

import dev.creategmf.config.GmfConfig;
import dev.creategmf.integration.ShaderStatusDetector;
import dev.creategmf.profiler.FrameStatistics;
import dev.creategmf.profiler.MemoryMetricsCollector;
import dev.creategmf.profiler.MemorySnapshot;
import dev.creategmf.profiler.ServerTickCollector;
import dev.creategmf.util.LongRingBuffer;

public final class DiagnosticSession {
    public static final DiagnosticSession INSTANCE = new DiagnosticSession();

    public enum State {
        IDLE,
        PREPARING,
        RUNNING,
        COMPLETE
    }

    private static final long PREPARATION_NANOS = 3_000_000_000L;

    private final LongRingBuffer frameSamples = new LongRingBuffer(4096);
    private State state = State.IDLE;
    private long startedNanos;
    private long durationNanos;
    private MemorySnapshot memoryBefore = MemorySnapshot.EMPTY;
    private DiagnosticsResult result;

    private DiagnosticSession() {
    }

    public void start() {
        frameSamples.clear();
        durationNanos = GmfConfig.CLIENT.diagnosticDurationSeconds.get() * 1_000_000_000L;
        startedNanos = System.nanoTime();
        result = null;
        state = State.PREPARING;
    }

    public void cancel() {
        if (isActive()) {
            state = State.IDLE;
            frameSamples.clear();
        }
    }

    public void onFrame(long frameNanos) {
        if (state == State.RUNNING && frameNanos > 0) {
            frameSamples.add(frameNanos);
        }
    }

    public void tick() {
        long elapsed = System.nanoTime() - startedNanos;
        if (state == State.PREPARING && elapsed >= PREPARATION_NANOS) {
            MemoryMetricsCollector.INSTANCE.sample();
            memoryBefore = MemoryMetricsCollector.INSTANCE.snapshot();
            frameSamples.clear();
            startedNanos = System.nanoTime();
            state = State.RUNNING;
            return;
        }
        if (state != State.RUNNING || elapsed < durationNanos) {
            return;
        }
        finish();
    }

    private void finish() {
        MemoryMetricsCollector.INSTANCE.sample();
        MemorySnapshot memoryAfter = MemoryMetricsCollector.INSTANCE.snapshot();
        FrameStatistics frames = FrameStatistics.fromNanos(frameSamples.snapshot());
        double mspt = ServerTickCollector.averageMspt();
        SceneCensus scene = CreateSceneScanner.captureNearby();
        Classification classification = classify(frames, mspt, memoryBefore, memoryAfter, scene);
        result = new DiagnosticsResult(frames, mspt, memoryBefore, memoryAfter, scene,
                classification.type, classification.evidence, classification.confidence, classification.reasonKey);
        state = State.COMPLETE;
    }

    private static Classification classify(FrameStatistics frames, double mspt, MemorySnapshot before,
            MemorySnapshot after, SceneCensus scene) {
        int target = GmfConfig.CLIENT.targetFps.get();
        boolean frameBound = frames.samples() >= 60 && frames.averageFps() < target * 0.85;
        long gcDelta = Math.max(0, after.gcCount() - before.gcCount());

        if (mspt >= 45) {
            return new Classification(BottleneckType.SERVER_TICK, EvidenceStatus.MEASURED, Confidence.HIGH,
                    "diagnostic.create_gmf.reason.high_mspt");
        }
        if (after.heapUsageRatio() >= 0.85 && gcDelta > 0) {
            return new Classification(BottleneckType.RAM_GC, EvidenceStatus.MEASURED, Confidence.MEDIUM,
                    "diagnostic.create_gmf.reason.memory_pressure");
        }
        if (!frameBound) {
            return new Classification(BottleneckType.NONE, EvidenceStatus.MEASURED, Confidence.HIGH,
                    "diagnostic.create_gmf.reason.target_met");
        }

        if (ShaderStatusDetector.isShaderPackActive()) {
            return new Classification(BottleneckType.SHADERS, EvidenceStatus.INFERRED, Confidence.MEDIUM,
                    "diagnostic.create_gmf.reason.shaders_and_frame_time");
        }
        if (scene.transportedItems() >= 64) {
            return new Classification(BottleneckType.TRANSPORTED_ITEMS, EvidenceStatus.ESTIMATED, Confidence.MEDIUM,
                    "diagnostic.create_gmf.reason.many_transported_items");
        }
        if (scene.contraptions() >= 3) {
            return new Classification(BottleneckType.CONTRAPTIONS, EvidenceStatus.ESTIMATED, Confidence.MEDIUM,
                    "diagnostic.create_gmf.reason.many_contraptions");
        }
        if (scene.createBlockEntities() >= 100) {
            return new Classification(BottleneckType.CREATE_RENDERING, EvidenceStatus.ESTIMATED, Confidence.LOW,
                    "diagnostic.create_gmf.reason.dense_create_scene");
        }
        return new Classification(BottleneckType.UNKNOWN, EvidenceStatus.INFERRED, Confidence.LOW,
                "diagnostic.create_gmf.reason.no_attribution_timer");
    }

    public double progress() {
        if (state == State.COMPLETE) {
            return 1;
        }
        if (state == State.PREPARING) {
            return Math.min(1.0, (double) (System.nanoTime() - startedNanos) / PREPARATION_NANOS);
        }
        if (state != State.RUNNING || durationNanos == 0) {
            return 0;
        }
        return Math.min(1.0, (double) (System.nanoTime() - startedNanos) / durationNanos);
    }

    public State state() {
        return state;
    }

    public DiagnosticsResult result() {
        return result;
    }

    public boolean isActive() {
        return state == State.PREPARING || state == State.RUNNING;
    }

    private record Classification(BottleneckType type, EvidenceStatus evidence, Confidence confidence,
            String reasonKey) {
    }
}
