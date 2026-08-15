package dev.creategmf.config;

/** The black box intentionally exposes only implemented collection modes. */
public enum DeveloperLogLevel {
    NORMAL("enum.create_gmf.developer_log_level.normal");

    private final String translationKey;

    DeveloperLogLevel(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
