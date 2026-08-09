package dev.creategmf.profiler;

public record MemorySnapshot(
        long heapUsedBytes,
        long heapMaxBytes,
        long gcCount,
        long gcCollectionMilliseconds,
        long sampledAtNanos
) {
    public static final MemorySnapshot EMPTY = new MemorySnapshot(0, 0, 0, 0, 0);

    public double heapUsageRatio() {
        return heapMaxBytes <= 0 ? 0 : (double) heapUsedBytes / heapMaxBytes;
    }
}
