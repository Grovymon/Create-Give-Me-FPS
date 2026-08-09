package dev.creategmf.config;

public enum PcProfile {
    POTATO,
    LOW,
    MEDIUM,
    ABOVE_AVERAGE,
    HIGH,
    ULTRA,
    CUSTOM;

    public String translationKey() {
        return "enum.create_gmf.profile." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
