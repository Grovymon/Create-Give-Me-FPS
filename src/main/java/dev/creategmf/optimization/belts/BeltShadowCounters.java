package dev.creategmf.optimization.belts;

public record BeltShadowCounters(long attempted, long rendered, long skipped) {
    public BeltShadowCounters subtract(BeltShadowCounters earlier) {
        return new BeltShadowCounters(attempted - earlier.attempted, rendered - earlier.rendered,
                skipped - earlier.skipped);
    }
}
