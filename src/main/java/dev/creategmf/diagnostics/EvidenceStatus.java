package dev.creategmf.diagnostics;

public enum EvidenceStatus {
    MEASURED,
    ESTIMATED,
    INFERRED,
    UNAVAILABLE;

    public String translationKey() {
        return "enum.create_gmf.evidence." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
