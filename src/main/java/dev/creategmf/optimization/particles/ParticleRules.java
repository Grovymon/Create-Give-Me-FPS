package dev.creategmf.optimization.particles;
/** Origin is required, not inferred from particle appearance or nearby blocks. */
public final class ParticleRules {
    private ParticleRules() {}
    public static boolean registeredCreateParticle(String namespace) { return "create".equals(namespace); }
}
