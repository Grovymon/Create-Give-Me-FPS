package dev.creategmf.config;

public enum ProfilerMode {
    OFF,
    LIGHT,
    DETAILED;

    public String translationKey() {
        return "enum.create_gmf.profiler." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
