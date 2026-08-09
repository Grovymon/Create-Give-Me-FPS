package dev.creategmf.profiler;

import java.util.concurrent.atomic.AtomicLong;

import dev.creategmf.CreateGmf;
import dev.creategmf.util.LongRingBuffer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = CreateGmf.MOD_ID)
public final class ServerTickCollector {
    private static final LongRingBuffer SAMPLES = new LongRingBuffer(1200);
    private static final AtomicLong TOTAL_NANOS = new AtomicLong();
    private static final AtomicLong TOTAL_SAMPLES = new AtomicLong();
    private static long tickStartNanos;

    private ServerTickCollector() {
    }

    @SubscribeEvent
    public static void onServerTickPre(ServerTickEvent.Pre event) {
        tickStartNanos = System.nanoTime();
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        long start = tickStartNanos;
        if (start != 0) {
            long duration = System.nanoTime() - start;
            SAMPLES.add(duration);
            TOTAL_NANOS.addAndGet(duration);
            TOTAL_SAMPLES.incrementAndGet();
        }
    }

    public static double averageMspt() {
        long[] values = SAMPLES.snapshot();
        if (values.length == 0) {
            return 0;
        }
        double sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum / values.length / 1_000_000.0;
    }

    public static int sampleCount() {
        return SAMPLES.size();
    }

    public static Totals totals() {
        return new Totals(TOTAL_NANOS.get(), TOTAL_SAMPLES.get());
    }

    public static double averageBetween(Totals before, Totals after) {
        long samples = after.samples - before.samples;
        return samples <= 0 ? 0 : (after.nanos - before.nanos) / samples / 1_000_000.0;
    }

    public record Totals(long nanos, long samples) {
    }
}
