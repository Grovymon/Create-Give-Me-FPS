package dev.creategmf.recommendation;

import java.util.List;

import dev.creategmf.config.GmfConfig;
import dev.creategmf.diagnostics.BottleneckType;
import dev.creategmf.diagnostics.Confidence;
import dev.creategmf.diagnostics.DiagnosticsResult;

public final class RecommendationEngine {
    private RecommendationEngine() {
    }

    public static List<Recommendation> recommendations(DiagnosticsResult result) {
        if (result == null || result.scene().transportedItems() < 16) {
            return List.of();
        }
        BottleneckType type = result.primaryBottleneck();
        if (type != BottleneckType.TRANSPORTED_ITEMS && type != BottleneckType.CREATE_RENDERING) {
            return List.of();
        }
        double current = GmfConfig.CLIENT.beltItemShadowDistance.get();
        double proposed = Math.min(current, 16.0);
        if (proposed >= current) {
            return List.of();
        }
        return List.of(new Recommendation(
                "config.create_gmf.belt_item_shadow_distance",
                current,
                proposed,
                "enum.create_gmf.impact.medium",
                "enum.create_gmf.impact.low",
                Confidence.MEDIUM,
                "recommendation.create_gmf.reason.belt_item_shadows",
                Recommendation.Action.BELT_SHADOW_DISTANCE));
    }

    public static void apply(Recommendation recommendation) {
        if (recommendation.action() == Recommendation.Action.BELT_SHADOW_DISTANCE) {
            GmfConfig.CLIENT.beltItemShadowOptimization.set(true);
            GmfConfig.CLIENT.beltItemShadowDistance.set(recommendation.proposedValue());
            GmfConfig.save();
        }
    }
}
