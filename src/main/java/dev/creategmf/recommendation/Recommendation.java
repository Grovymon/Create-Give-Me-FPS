package dev.creategmf.recommendation;

import dev.creategmf.diagnostics.Confidence;

public record Recommendation(
        String settingTranslationKey,
        double currentValue,
        double proposedValue,
        String expectedImpactTranslationKey,
        String visualImpactTranslationKey,
        Confidence confidence,
        String reasonTranslationKey,
        Action action,
        long evidenceCount
) {
    public enum Action {
        BELT_SHADOW_DISTANCE, TRANSPORTED_ITEMS, OFFSCREEN, OCCLUSION, PARTICLES, VISUAL_TICK_DIVISOR,
        RENDERER_EXPERIMENT
    }
}
