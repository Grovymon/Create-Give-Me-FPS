package dev.creategmf.optimization.animations;

/** Ignore two consecutive misses; visibility/unknown resumes immediately. One observation per frame. */
public final class FrustumGrace {
    private long frame = Long.MIN_VALUE;
    private int misses;
    public boolean paused(long currentFrame, boolean visibleOrUnknown) {
        if (visibleOrUnknown) { misses = 0; frame = currentFrame; return false; }
        if (frame != currentFrame) { frame = currentFrame; misses = Math.min(3, misses + 1); }
        return misses >= 3;
    }
}
