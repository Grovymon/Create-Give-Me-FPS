package dev.creategmf.profiler;

import dev.creategmf.util.LongRingBuffer;

public final class FrameTimeCollector {
    public static final FrameTimeCollector INSTANCE = new FrameTimeCollector();
    private static final long MAX_VALID_FRAME_NANOS = 1_000_000_000L;
    private static final long LIVE_WINDOW_NANOS = 2_000_000_000L;

    private final LongRingBuffer samples = new LongRingBuffer(4096);
    private final LongRingBuffer sampleTimes = new LongRingBuffer(4096);
    private long previousFrameNanos;

    private FrameTimeCollector() {
    }

    public long recordFrame(long nowNanos) {
        long previous = previousFrameNanos;
        previousFrameNanos = nowNanos;
        if (previous == 0) {
            return 0;
        }
        long duration = nowNanos - previous;
        if (duration > 0 && duration < MAX_VALID_FRAME_NANOS) {
            samples.add(duration);
            sampleTimes.add(nowNanos);
            return duration;
        }
        return 0;
    }

    public FrameStatistics snapshot() {
        long[] durations = samples.snapshot();
        long[] times = sampleTimes.snapshot();
        long cutoff = System.nanoTime() - LIVE_WINDOW_NANOS;
        int first = 0;
        while (first < times.length && times[first] < cutoff) {
            first++;
        }
        if (first == 0) {
            return FrameStatistics.fromNanos(durations);
        }
        long[] recent = new long[durations.length - first];
        System.arraycopy(durations, first, recent, 0, recent.length);
        return FrameStatistics.fromNanos(recent);
    }
}
