package dev.creategmf.recommendation;

import java.util.List;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.CreateParticleMode;
import dev.creategmf.config.PcProfile;
import dev.creategmf.diagnostics.Confidence;
import dev.creategmf.diagnostics.DiagnosticsResult;
import dev.creategmf.integration.ShaderStatusDetector;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;

public final class RecommendationEngine {
    private RecommendationEngine() {}
    public static List<Recommendation> recommendations(DiagnosticsResult result) {
        if (result == null) return List.of();
        var c = GmfConfig.CLIENT;
        var evidence = result.render();
        var input = new RecommendationRules.Input(result.frames().samples() >= 60
                && result.frames().averageFps() < c.targetFps.get() * .85,
                result.scene().transportedItems(), evidence.population().offScreen(), evidence.occluded(),
                evidence.particleRequests(), result.scene().contraptionBlocks(), ShaderStatusDetector.isShaderPackActive(),
                c.renderTransportedBeltItems.get(), c.beltItemShadowDistance.get(),
                c.pauseOffScreenAnimations.get(), c.createOcclusionCulling.get(), c.createParticleMode.get().ordinal(),
                c.animationUpdateTickDivisor.get());
        return RecommendationRules.evaluate(input).stream().map(candidate -> {
            String setting = switch (candidate.action()) {
                case BELT_SHADOW_DISTANCE -> "belt_item_shadow_distance";
                case TRANSPORTED_ITEMS -> "render_transported_belt_items";
                case OFFSCREEN -> "pause_off_screen";
                case OCCLUSION -> "occlusion_culling";
                case PARTICLES -> "create_particles";
                case VISUAL_TICK_DIVISOR -> "animation_update_ticks";
                case RENDERER_EXPERIMENT -> "flywheel_backend";
            };
            return new Recommendation("config.create_gmf." + setting, candidate.current(), candidate.proposed(),
                    "enum.create_gmf.impact.medium", "enum.create_gmf.impact."
                    + (candidate.action() == Recommendation.Action.TRANSPORTED_ITEMS ? "high" : "low"),
                    Confidence.MEDIUM, "recommendation.create_gmf.reason."
                    + candidate.action().name().toLowerCase(java.util.Locale.ROOT), candidate.action(), candidate.evidence());
        }).toList();
    }
    public static void apply(Recommendation recommendation) {
        var c = GmfConfig.CLIENT;
        switch (recommendation.action()) {
            case BELT_SHADOW_DISTANCE -> { c.beltItemShadowOptimization.set(true); c.beltItemShadowDistance.set(recommendation.proposedValue()); }
            case TRANSPORTED_ITEMS -> c.renderTransportedBeltItems.set(false);
            case OFFSCREEN -> c.pauseOffScreenAnimations.set(true);
            case OCCLUSION -> c.createOcclusionCulling.set(true);
            case PARTICLES -> c.createParticleMode.set(CreateParticleMode.REDUCED);
            case VISUAL_TICK_DIVISOR -> c.animationUpdateTickDivisor.set((int) recommendation.proposedValue());
            case RENDERER_EXPERIMENT -> { return; } // Informational restart experiment, not an automatic backend selection.
        }
        c.pcProfile.set(PcProfile.CUSTOM);
        GmfConfig.save();
        DistantAnimationController.refreshLoadedKinetics();
        CreateOcclusionController.onConfigurationChanged();
    }
}
