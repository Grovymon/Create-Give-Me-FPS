package dev.creategmf.profiler;

import java.util.Arrays;

public record FrameStatistics(
        int samples,
        double averageMilliseconds,
        double medianMilliseconds,
        double minimumMilliseconds,
        double maximumMilliseconds,
        double varianceMillisecondsSquared,
        double averageFps,
        double onePercentLow,
        double pointOnePercentLow
) {
    public static final FrameStatistics EMPTY = new FrameStatistics(0, 0, 0, 0, 0, 0, 0, 0, 0);
    private static final double NANOS_PER_MILLI = 1_000_000.0;

    public static FrameStatistics fromNanos(long[] nanos) {
        if (nanos.length == 0) {
            return EMPTY;
        }

        long[] sorted = nanos.clone();
        Arrays.sort(sorted);
        double sum = 0;
        for (long value : nanos) {
            sum += value;
        }
        double averageNanos = sum / nanos.length;
        double varianceNanos = 0;
        for (long value : nanos) {
            double delta = value - averageNanos;
            varianceNanos += delta * delta;
        }
        varianceNanos /= nanos.length;

        double medianNanos;
        int middle = sorted.length / 2;
        if ((sorted.length & 1) == 0) {
            medianNanos = (sorted[middle - 1] + sorted[middle]) / 2.0;
        } else {
            medianNanos = sorted[middle];
        }

        double oneLow = sorted.length >= 300 ? lowFps(sorted, 0.01) : 0;
        double pointOneLow = sorted.length >= 1000 ? lowFps(sorted, 0.001) : 0;
        return new FrameStatistics(
                sorted.length,
                averageNanos / NANOS_PER_MILLI,
                medianNanos / NANOS_PER_MILLI,
                sorted[0] / NANOS_PER_MILLI,
                sorted[sorted.length - 1] / NANOS_PER_MILLI,
                varianceNanos / (NANOS_PER_MILLI * NANOS_PER_MILLI),
                fpsFromNanos(averageNanos),
                oneLow,
                pointOneLow
        );
    }

    private static double lowFps(long[] sorted, double fraction) {
        int count = Math.max(1, (int) Math.ceil(sorted.length * fraction));
        double totalNanos = 0;
        for (int index = sorted.length - count; index < sorted.length; index++) {
            totalNanos += sorted[index];
        }
        return fpsFromNanos(totalNanos / count);
    }

    private static double fpsFromNanos(double nanos) {
        return nanos <= 0 ? 0 : 1_000_000_000.0 / nanos;
    }
}
