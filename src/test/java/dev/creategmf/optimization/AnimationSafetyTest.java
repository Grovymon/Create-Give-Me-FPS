package dev.creategmf.optimization;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import dev.creategmf.optimization.animations.*;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;
import dev.creategmf.config.DistantAnimationMode;

class AnimationSafetyTest {
    @Test void distanceRoundTripResumes() {
        assertEquals(AnimationPolicy.FULL, policy(16, false));
        assertEquals(AnimationPolicy.STATIC, policy(9216, false));
        assertEquals(AnimationPolicy.FULL, policy(16, false));
    }
    @Test void disabledGmfAndUnknownOwnershipFailOpen() {
        assertEquals(AnimationPolicy.FULL, AnimationRules.evaluate(false, true, false, 0, 9000, DistantAnimationMode.STATIC, true));
        assertEquals(AnimationPolicy.FULL, AnimationRules.evaluate(true, false, false, 0, Double.NaN, DistantAnimationMode.STATIC, true));
    }
    @Test void reducedBandAndBoundaries() {
        assertEquals(AnimationPolicy.FULL, policy(32 * 32, false));
        assertEquals(AnimationPolicy.REDUCED, policy(64 * 64, false));
        assertEquals(AnimationPolicy.STATIC, policy(65 * 65, false));
        assertEquals(AnimationPolicy.STATIC, policy(1, true));
    }
    @Test void frustumEdgesHaveGraceButReturnIsImmediate() {
        FrustumGrace grace = new FrustumGrace();
        assertFalse(grace.paused(1, false));
        assertFalse(grace.paused(1, false));
        assertFalse(grace.paused(2, false));
        assertTrue(grace.paused(3, false));
        assertFalse(grace.paused(3, true));
        assertFalse(grace.paused(4, false));
    }
    @Test void shaderFreezeDoesNotReplaceSourceSpeed() {
        ShaderAnimationSource source = new ShaderAnimationSource();
        source.capture(96, 45);
        assertEquals(0, source.renderedSpeed(true));
        assertEquals(96, source.renderedSpeed(false));
        assertEquals(45, source.offset());
        source.speed(0); // Real Create RPM becomes zero while GMF has paused the shader.
        assertEquals(0, source.renderedSpeed(false));
        source.speed(-64);
        assertEquals(-64, source.renderedSpeed(false));
    }
    private static AnimationPolicy policy(double distance, boolean outside) {
        return AnimationRules.evaluate(true, true, true, 32, distance, DistantAnimationMode.REDUCED, outside);
    }
}
