package dev.creategmf.optimization.occlusion;

/** GMF queue ownership, distinct from EC visibility. Pure regression-testable transition state. */
public final class HiddenVisualState {
    public enum Action { NONE, REMOVE, ADD }
    private boolean owesRestore;
    public void creationRejected() { owesRestore = true; }
    public boolean owesRestore() { return owesRestore; }
    public Action transition(boolean active, boolean knownHidden) {
        boolean hidden = active && knownHidden;
        if (hidden && !owesRestore) { owesRestore = true; return Action.REMOVE; }
        if (!hidden && owesRestore) { owesRestore = false; return Action.ADD; }
        return Action.NONE;
    }
}
