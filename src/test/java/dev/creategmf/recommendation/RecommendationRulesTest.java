package dev.creategmf.recommendation;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class RecommendationRulesTest {
    @Test void noPressureDoesNotRecommendDowngrades() {
        assertTrue(RecommendationRules.evaluate(input(false)).isEmpty());
    }
    @Test void countsProduceSeveralCandidatesNotFakeTimes() {
        var candidates = RecommendationRules.evaluate(input(true));
        assertEquals(7, candidates.size());
        assertTrue(candidates.stream().allMatch(candidate -> candidate.evidence() > 0));
    }
    @Test void adaptiveRequiresSustainedPressureAndCooldown() {
        var gate = new AdaptiveRecommendationGate();
        assertEquals(AdaptiveRecommendationGate.Proposal.NONE, gate.observe(30, 60, 0));
        assertEquals(AdaptiveRecommendationGate.Proposal.NONE, gate.observe(30, 60, 19));
        assertEquals(AdaptiveRecommendationGate.Proposal.LOWER_QUALITY, gate.observe(30, 60, 20));
        assertEquals(AdaptiveRecommendationGate.Proposal.NONE, gate.observe(30, 60, 40));
        assertEquals(AdaptiveRecommendationGate.Proposal.NONE, gate.observe(60, 60, 50));
        assertEquals(AdaptiveRecommendationGate.Proposal.NONE, gate.observe(90, 60, 120));
        assertEquals(AdaptiveRecommendationGate.Proposal.RESTORE_QUALITY, gate.observe(90, 60, 165));
    }
    private static RecommendationRules.Input input(boolean pressure) {
        return new RecommendationRules.Input(pressure, 64, 20, 20, 240, 256, true, true, 24, false, false, 0, 1);
    }
}
