package dev.creategmf.benchmark;
import java.util.Locale;
public enum OptimizationTarget {
    BELT_SHADOWS, OCCLUSION, OFFSCREEN, DISTANT, TRANSPORTED_ITEMS, PARTICLES, ACCELERATED_RENDERER, FLYWHEEL_BACKEND;
    public boolean runtimeSafe() { return this != ACCELERATED_RENDERER && this != FLYWHEEL_BACKEND; }
    public String translationKey() { return "benchmark.create_gmf.target." + name().toLowerCase(Locale.ROOT); }
}
