package dev.creategmf.optimization.animations;

import dev.creategmf.config.DistantAnimationMode;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;

/** Pure policy. Unknown ownership, position or camera always fails open. */
public final class AnimationRules {
    private AnimationRules() {}
    public static AnimationPolicy evaluate(boolean enabled, boolean known, boolean groupEnabled,
            double radius, double squaredDistance, DistantAnimationMode mode, boolean offScreen) {
        if (!enabled || !known || !Double.isFinite(squaredDistance)) return AnimationPolicy.FULL;
        if (!groupEnabled || radius <= 0 || offScreen) return AnimationPolicy.STATIC;
        if (mode == DistantAnimationMode.FULL || squaredDistance <= radius * radius) return AnimationPolicy.FULL;
        return mode == DistantAnimationMode.REDUCED && squaredDistance <= radius * radius * 4
                ? AnimationPolicy.REDUCED : AnimationPolicy.STATIC;
    }
}
