package dev.creategmf.optimization.belts;

/** Counts renderItems calls (not stack count or CPU/GPU milliseconds). Render thread only. */
public final class TransportedItemCounters {
    private static long attempted;
    private static long skipped;
    private TransportedItemCounters() {}
    public static void attempt() { attempted++; }
    public static void skip() { skipped++; }
    public static Snapshot snapshot() { return new Snapshot(attempted, skipped); }
    public record Snapshot(long attempted, long skipped) {}
}
