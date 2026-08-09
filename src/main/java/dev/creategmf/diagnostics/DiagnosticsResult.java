package dev.creategmf.diagnostics;

import dev.creategmf.profiler.FrameStatistics;
import dev.creategmf.profiler.MemorySnapshot;

public record DiagnosticsResult(
        FrameStatistics frames,
        double mspt,
        MemorySnapshot memoryBefore,
        MemorySnapshot memoryAfter,
        SceneCensus scene,
        BottleneckType primaryBottleneck,
        EvidenceStatus evidence,
        Confidence confidence,
        String reasonTranslationKey
) {
}
