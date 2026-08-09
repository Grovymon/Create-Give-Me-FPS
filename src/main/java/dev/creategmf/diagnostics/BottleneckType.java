package dev.creategmf.diagnostics;

public enum BottleneckType {
    NONE,
    GPU,
    CPU_MAIN_THREAD,
    SERVER_TICK,
    SHADERS,
    CREATE_RENDERING,
    CREATE_SIMULATION,
    KINETIC,
    BELTS,
    TRANSPORTED_ITEMS,
    CONTRAPTIONS,
    BLOCK_ENTITIES,
    PARTICLES,
    FLUID_RENDERING,
    CHUNK_RENDERING,
    RAM_GC,
    VRAM_PRESSURE,
    MIXED,
    UNKNOWN;

    public String translationKey() {
        return "diagnostic.create_gmf.bottleneck." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
