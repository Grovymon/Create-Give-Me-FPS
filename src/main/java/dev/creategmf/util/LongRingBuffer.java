package dev.creategmf.util;

public final class LongRingBuffer {
    private final long[] values;
    private volatile int cursor;
    private volatile int size;

    public LongRingBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        values = new long[capacity];
    }

    public void add(long value) {
        int write = cursor;
        values[write] = value;
        int next = write + 1;
        if (next == values.length) {
            next = 0;
        }
        cursor = next;
        if (size < values.length) {
            size++;
        }
    }

    public int size() {
        return size;
    }

    public long[] snapshot() {
        int count = size;
        long[] copy = new long[count];
        int start = cursor - count;
        if (start < 0) {
            start += values.length;
        }
        int first = Math.min(count, values.length - start);
        System.arraycopy(values, start, copy, 0, first);
        if (first < count) {
            System.arraycopy(values, 0, copy, first, count - first);
        }
        return copy;
    }

    public void clear() {
        cursor = 0;
        size = 0;
    }
}
