package dev.creategmf.optimization;

import static org.junit.jupiter.api.Assertions.*;
import dev.creategmf.config.DistantAnimationMode;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.AnimationPolicyRules;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;
import org.junit.jupiter.api.Test;

class AnimationRegressionTest {
    @Test void everyDisabledGroupRemainsStaticEvenWithUnknownPositionAndFullMode() {
        for (var group : MechanismAnimationGroup.values()) {
            for (var mode : DistantAnimationMode.values()) {
                assertEquals(AnimationPolicy.STATIC,
                        AnimationPolicyRules.evaluate(true, false, 32, false, Double.NaN, mode),
                        group + " / " + mode);
            }
        }
    }

    @Test void zeroRadiusOverridesAllModesAndMissingOwnershipData() {
        for (var mode : DistantAnimationMode.values()) {
            assertEquals(AnimationPolicy.STATIC,
                    AnimationPolicyRules.evaluate(true, true, 0, false, Double.NaN, mode));
            assertEquals(AnimationPolicy.STATIC,
                    AnimationPolicyRules.evaluate(true, true, 0, true, 0, mode));
        }
    }

    @Test void disabledGmfRestoresFullAnimation() {
        for (var mode : DistantAnimationMode.values()) {
            assertEquals(AnimationPolicy.FULL,
                    AnimationPolicyRules.evaluate(false, false, 0, false, Double.NaN, mode));
        }
    }

    @Test void missingPositionDoesNotForceFullAnimationInStaticMode() {
        assertEquals(AnimationPolicy.STATIC,
                AnimationPolicyRules.evaluate(true, true, 32, false, Double.NaN, DistantAnimationMode.STATIC));
        assertEquals(AnimationPolicy.REDUCED,
                AnimationPolicyRules.evaluate(true, true, 32, false, Double.NaN, DistantAnimationMode.REDUCED));
    }

    @Test void movingAwayAndReturningUsesTheOriginalDistanceBands() {
        assertEquals(AnimationPolicy.FULL, at(16));
        assertEquals(AnimationPolicy.FULL, at(32 * 32));
        assertEquals(AnimationPolicy.REDUCED, at(64 * 64));
        assertEquals(AnimationPolicy.STATIC, at(65 * 65));
        assertEquals(AnimationPolicy.FULL, at(16));
    }

    @Test void unregisteredScrollingStillFreezesAtZeroRadius() {
        for (var mode : DistantAnimationMode.values()) {
            assertTrue(AnimationPolicyRules.freezeUnclassifiedScroll(true, 0, false, mode, false, Double.NaN));
            assertTrue(AnimationPolicyRules.freezeUnclassifiedScroll(true, 32, true, mode, false, Double.NaN));
        }
    }

    @Test void unregisteredScrollingDoesNotStaySuppressedWhenGmfIsDisabled() {
        assertFalse(AnimationPolicyRules.freezeUnclassifiedScroll(false, 0, true,
                DistantAnimationMode.STATIC, true, 10000));
    }

    @Test void fallbackScrollingKeepsOriginalDistancePolicy() {
        assertFalse(AnimationPolicyRules.freezeUnclassifiedScroll(true, 32, false,
                DistantAnimationMode.STATIC, true, 32 * 32));
        assertTrue(AnimationPolicyRules.freezeUnclassifiedScroll(true, 32, false,
                DistantAnimationMode.STATIC, true, 33 * 33));
        assertFalse(AnimationPolicyRules.freezeUnclassifiedScroll(true, 32, false,
                DistantAnimationMode.FULL, true, 10000));
    }

    private static AnimationPolicy at(double squaredDistance) {
        return AnimationPolicyRules.evaluate(true, true, 32, true, squaredDistance, DistantAnimationMode.REDUCED);
    }
}
