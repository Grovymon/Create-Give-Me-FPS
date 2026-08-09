package dev.creategmf.diagnostics;

public record SceneCensus(
        int chunksScanned,
        int createBlockEntities,
        int kineticBlockEntities,
        int beltControllers,
        int transportedItems,
        int contraptions
) {
    public static final SceneCensus EMPTY = new SceneCensus(0, 0, 0, 0, 0, 0);
}
