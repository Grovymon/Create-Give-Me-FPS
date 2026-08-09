package dev.creategmf.benchmark;

import dev.creategmf.optimization.belts.BeltShadowCounters;
import dev.creategmf.profiler.FrameStatistics;

public record OptimizationBenchmarkResult(
        FrameStatistics beforeFrames,
        FrameStatistics afterFrames,
        double beforeMspt,
        double afterMspt,
        BeltShadowCounters beforeCounters,
        BeltShadowCounters afterCounters
) {
}
