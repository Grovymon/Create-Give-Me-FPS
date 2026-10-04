package dev.creategmf.config;
/** Pure preset matrix used by configuration and regression tests. */
public record ProfileRenderingValues(int targetFps, double radius, int animationFps, double shadowRadius,
        CreateParticleMode particles, DistantAnimationMode mode) {
    public static ProfileRenderingValues forProfile(PcProfile profile) {
        return switch (profile) {
            case POTATO -> new ProfileRenderingValues(45, 0, 5, 0, CreateParticleMode.OFF, DistantAnimationMode.STATIC);
            case LOW -> new ProfileRenderingValues(60, 16, 5, 0, CreateParticleMode.OFF, DistantAnimationMode.STATIC);
            case MEDIUM -> new ProfileRenderingValues(75, 32, 10, 16, CreateParticleMode.REDUCED, DistantAnimationMode.REDUCED);
            case ABOVE_AVERAGE -> new ProfileRenderingValues(90, 32, 15, 24, CreateParticleMode.REDUCED, DistantAnimationMode.REDUCED);
            case HIGH -> new ProfileRenderingValues(120, 32, 20, 48, CreateParticleMode.FULL, DistantAnimationMode.REDUCED);
            case ULTRA -> new ProfileRenderingValues(144, 32, 30, 128, CreateParticleMode.FULL, DistantAnimationMode.FULL);
            case CUSTOM -> throw new IllegalArgumentException("Custom values must be preserved");
        };
    }
}
