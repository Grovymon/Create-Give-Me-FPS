package dev.creategmf.config;

public enum CreateParticleMode {
    FULL("enum.create_gmf.particles.full"),
    REDUCED("enum.create_gmf.particles.reduced"),
    OFF("enum.create_gmf.particles.off");

    private final String translationKey;

    CreateParticleMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
