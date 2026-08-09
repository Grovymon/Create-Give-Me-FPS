package dev.creategmf.config;

public enum DistantAnimationMode {
    FULL("enum.create_gmf.animations.full"),
    REDUCED("enum.create_gmf.animations.reduced"),
    STATIC("enum.create_gmf.animations.static");

    private final String translationKey;

    DistantAnimationMode(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
