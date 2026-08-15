package dev.creategmf.diagnostics;

/**
 * Runtime evidence for hooks that cannot be inferred from a checked checkbox.
 * Flags become true only after the corresponding path was actually reached.
 */
public final class GmfRuntimeStatus {
    private static volatile boolean animationHookObserved;
    private static volatile boolean beltItemHookObserved;
    private static volatile boolean particleHookObserved;

    private GmfRuntimeStatus() {
    }

    public static void markAnimationHook() {
        animationHookObserved = true;
    }

    public static void markBeltItemHook() {
        beltItemHookObserved = true;
    }

    public static void markParticleHook() {
        particleHookObserved = true;
    }

    public static boolean animationHookObserved() {
        return animationHookObserved;
    }

    public static boolean beltItemHookObserved() {
        return beltItemHookObserved;
    }

    public static boolean particleHookObserved() {
        return particleHookObserved;
    }
}
