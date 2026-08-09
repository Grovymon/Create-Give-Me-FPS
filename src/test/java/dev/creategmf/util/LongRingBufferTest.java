package dev.creategmf.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LongRingBufferTest {
    @Test
    void keepsInsertionOrderBeforeAndAfterWrap() {
        LongRingBuffer buffer = new LongRingBuffer(3);
        buffer.add(10);
        buffer.add(20);
        assertArrayEquals(new long[] {10, 20}, buffer.snapshot());

        buffer.add(30);
        buffer.add(40);
        assertEquals(3, buffer.size());
        assertArrayEquals(new long[] {20, 30, 40}, buffer.snapshot());
    }

    @Test
    void clearResetsVisibleSamples() {
        LongRingBuffer buffer = new LongRingBuffer(2);
        buffer.add(1);
        buffer.clear();
        assertEquals(0, buffer.size());
        assertArrayEquals(new long[0], buffer.snapshot());
    }

    @Test
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new LongRingBuffer(0));
    }
}
