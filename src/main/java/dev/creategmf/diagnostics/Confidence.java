package dev.creategmf.diagnostics;

public enum Confidence {
    LOW,
    MEDIUM,
    HIGH;

    public String translationKey() {
        return "enum.create_gmf.confidence." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
