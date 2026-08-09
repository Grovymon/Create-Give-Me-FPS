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
        Action action
) {
    public enum Action {
        BELT_SHADOW_DISTANCE
    }
}
