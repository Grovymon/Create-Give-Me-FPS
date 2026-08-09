package dev.creategmf.benchmark;

import dev.creategmf.config.PcProfile;
import dev.creategmf.diagnostics.EvidenceStatus;
import dev.creategmf.profiler.FrameStatistics;
import dev.creategmf.profiler.MemorySnapshot;

public record BenchmarkResult(
        double cpuOperationsPerMillisecond,
        FrameStatistics frames,
        MemorySnapshot memoryBefore,
        MemorySnapshot memoryAfter,
        PcProfile profile,
        EvidenceStatus profileEvidence
) {
}
