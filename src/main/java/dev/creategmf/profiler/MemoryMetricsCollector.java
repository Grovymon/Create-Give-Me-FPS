package dev.creategmf.profiler;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public final class MemoryMetricsCollector {
    public static final MemoryMetricsCollector INSTANCE = new MemoryMetricsCollector();

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private volatile MemorySnapshot latest = MemorySnapshot.EMPTY;

    private MemoryMetricsCollector() {
    }

    public void sample() {
        long count = 0;
        long time = 0;
        for (GarbageCollectorMXBean bean : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (bean.getCollectionCount() >= 0) {
                count += bean.getCollectionCount();
            }
            if (bean.getCollectionTime() >= 0) {
                time += bean.getCollectionTime();
            }
        }
        var heap = memoryBean.getHeapMemoryUsage();
        latest = new MemorySnapshot(heap.getUsed(), heap.getMax(), count, time, System.nanoTime());
    }

    public MemorySnapshot snapshot() {
        return latest;
    }
}
