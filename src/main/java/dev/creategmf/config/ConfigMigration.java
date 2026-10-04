package dev.creategmf.config;
/** NeoForge spec supplies defaults/removes unknown keys; migration never resets user visual choices. */
public final class ConfigMigration {
    private ConfigMigration() {}
    public static int schema(int previous, int current) {
        if (previous > current) throw new IllegalArgumentException("Do not silently downgrade a newer config");
        return current;
    }
    public static double animationRadius(double value) {
        return Double.isFinite(value) ? Math.clamp(value, 0, 32) : 32;
    }
}
