package dev.creategmf.optimization.animations;

import dev.creategmf.config.DistantAnimationMode;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;

/** The 0.1.3 ordering: explicit user disables precede missing position/camera data. */
public final class AnimationPolicyRules {
    private AnimationPolicyRules() {}

    public static AnimationPolicy evaluate(boolean enabled, boolean groupEnabled, double radius,
            boolean positioned, double squaredDistance, DistantAnimationMode mode) {
        if (!enabled) return AnimationPolicy.FULL;
        if (!groupEnabled || radius <= 0) return AnimationPolicy.STATIC;
        if (mode == DistantAnimationMode.FULL) return AnimationPolicy.FULL;
        if (!positioned || !Double.isFinite(squaredDistance)) {
            return mode == DistantAnimationMode.REDUCED ? AnimationPolicy.REDUCED : AnimationPolicy.STATIC;
        }
        if (squaredDistance <= radius * radius) return AnimationPolicy.FULL;
        if (mode == DistantAnimationMode.REDUCED && squaredDistance <= radius * radius * 4) {
            return AnimationPolicy.REDUCED;
        }
        return AnimationPolicy.STATIC;
    }

    /** Preserve the fallback for Create ScrollInstances without a registered belt owner. */
    public static boolean freezeUnclassifiedScroll(boolean enabled, double radius, boolean allGroupsDisabled,
            DistantAnimationMode mode, boolean levelAvailable, double squaredDistance) {
        if (!enabled) return false;
        if (radius <= 0 || allGroupsDisabled) return true;
        if (mode == DistantAnimationMode.FULL || !levelAvailable) return false;
        return squaredDistance > radius * radius;
    }
}
