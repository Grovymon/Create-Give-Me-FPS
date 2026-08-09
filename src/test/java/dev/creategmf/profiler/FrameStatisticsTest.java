package dev.creategmf.profiler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class FrameStatisticsTest {
    private static final double EPSILON = 0.000_001;

    @Test
    void returnsEmptyForNoSamples() {
        assertEquals(FrameStatistics.EMPTY, FrameStatistics.fromNanos(new long[0]));
    }

    @Test
    void computesCoreStatisticsAndDoesNotMutateInput() {
        long[] input = {30_000_000L, 10_000_000L, 20_000_000L, 40_000_000L};
        long[] original = input.clone();

        FrameStatistics result = FrameStatistics.fromNanos(input);

        assertEquals(4, result.samples());
        assertEquals(25.0, result.averageMilliseconds(), EPSILON);
        assertEquals(25.0, result.medianMilliseconds(), EPSILON);
        assertEquals(10.0, result.minimumMilliseconds(), EPSILON);
        assertEquals(40.0, result.maximumMilliseconds(), EPSILON);
        assertEquals(125.0, result.varianceMillisecondsSquared(), EPSILON);
        assertEquals(40.0, result.averageFps(), EPSILON);
        assertEquals(0.0, result.onePercentLow(), EPSILON);
        assertEquals(0.0, result.pointOnePercentLow(), EPSILON);
        assertArrayEquals(original, input);
    }

    @Test
    void exposesLowPercentilesOnlyWithEnoughSamples() {
        long[] threeHundred = new long[300];
        Arrays.fill(threeHundred, 10_000_000L);
        Arrays.fill(threeHundred, 296, 300, 50_000_000L);
        FrameStatistics oneLow = FrameStatistics.fromNanos(threeHundred);
        assertEquals(20.0, oneLow.onePercentLow(), EPSILON);
        assertEquals(0.0, oneLow.pointOnePercentLow(), EPSILON);

        long[] thousand = new long[1000];
        Arrays.fill(thousand, 10_000_000L);
        Arrays.fill(thousand, 998, 1000, 100_000_000L);
        assertEquals(10.0, FrameStatistics.fromNanos(thousand).pointOnePercentLow(), EPSILON);
    }
}
