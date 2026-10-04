package dev.creategmf.recommendation;

import java.util.ArrayList;
import java.util.List;
import dev.creategmf.recommendation.Recommendation.Action;

/** Heuristic candidates based on measured counts, never alleged per-mechanism milliseconds. */
public final class RecommendationRules {
    private RecommendationRules() {}
    public record Input(boolean framePressure, int items, int offScreen, int occluded, long particleRequests,
            int contraptionBlocks, boolean shaderActive, boolean transported, double shadowDistance,
            boolean pause, boolean occlusion, int particleMode, int tickDivisor) {}
    public record Candidate(Action action, double current, double proposed, long evidence) {}
    public static List<Candidate> evaluate(Input input) {
        if (!input.framePressure) return List.of();
        List<Candidate> result = new ArrayList<>();
        if (input.items >= 16) {
            if (input.transported) result.add(new Candidate(Action.TRANSPORTED_ITEMS, 1, 0, input.items));
            if (input.shadowDistance > 16) result.add(new Candidate(Action.BELT_SHADOW_DISTANCE,
                    input.shadowDistance, 16, input.items));
        }
        if (input.offScreen >= 16 && !input.pause)
            result.add(new Candidate(Action.OFFSCREEN, 0, 1, input.offScreen));
        if (input.occluded >= 16 && !input.occlusion)
            result.add(new Candidate(Action.OCCLUSION, 0, 1, input.occluded));
        if (input.particleRequests >= 120 && input.particleMode == 0)
            result.add(new Candidate(Action.PARTICLES, 0, 1, input.particleRequests));
        if (input.contraptionBlocks >= 128 && input.tickDivisor < 3)
            result.add(new Candidate(Action.VISUAL_TICK_DIVISOR, input.tickDivisor, 3, input.contraptionBlocks));
        if (input.shaderActive) result.add(new Candidate(Action.RENDERER_EXPERIMENT, 0, 0, 1));
        return List.copyOf(result);
    }
}
