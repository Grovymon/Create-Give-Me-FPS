package dev.creategmf.recommendation;
/** Proposal gate only: sustained pressure/headroom, neutral reset, and a long cooldown. No config mutation. */
public final class AdaptiveRecommendationGate {
    public enum Proposal { NONE, LOWER_QUALITY, RESTORE_QUALITY }
    private long since = -1, cooldownUntil;
    private int direction;
    public Proposal observe(double fps, double target, long seconds) {
        if (!Double.isFinite(fps) || fps <= 0 || target <= 0) { since = -1; direction = 0; return Proposal.NONE; }
        int next = fps < target * .85 ? -1 : fps > target * 1.25 ? 1 : 0;
        if (next == 0) { since = -1; direction = 0; return Proposal.NONE; }
        if (next != direction || since < 0) { direction = next; since = seconds; }
        long sustained = next < 0 ? 20 : 45;
        if (seconds >= cooldownUntil && seconds - since >= sustained) {
            cooldownUntil = seconds + 120; since = seconds;
            return next < 0 ? Proposal.LOWER_QUALITY : Proposal.RESTORE_QUALITY;
        }
        return Proposal.NONE;
    }
}
