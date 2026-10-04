package dev.creategmf.optimization;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import dev.creategmf.optimization.occlusion.HiddenVisualState;
import dev.creategmf.optimization.occlusion.HiddenVisualState.Action;
class OcclusionStateTest {
    @Test void firstVisibleDoesNotAddAndTransitionsAreUnique() {
        var state = new HiddenVisualState();
        assertEquals(Action.NONE, state.transition(true, false));
        assertEquals(Action.REMOVE, state.transition(true, true));
        assertEquals(Action.NONE, state.transition(true, true));
        assertEquals(Action.ADD, state.transition(true, false));
        assertEquals(Action.NONE, state.transition(true, false));
    }
    @Test void rejectedCreationOwesOneRestoreButNotAnotherRemoval() {
        var state = new HiddenVisualState();
        state.creationRejected();
        assertEquals(Action.NONE, state.transition(true, true));
        assertEquals(Action.ADD, state.transition(true, false));
        assertEquals(Action.NONE, state.transition(true, false));
    }
    @Test void disableOrUnknownVisibilityRestores() {
        var state = new HiddenVisualState();
        state.transition(true, true);
        assertEquals(Action.ADD, state.transition(false, true));
        assertFalse(state.owesRestore());
    }
}
