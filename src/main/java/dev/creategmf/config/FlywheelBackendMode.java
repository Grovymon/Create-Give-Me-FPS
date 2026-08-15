package dev.creategmf.config;

public enum FlywheelBackendMode {
    DEFAULT("DEFAULT"),
    INDIRECT("flywheel:indirect"),
    INSTANCING("flywheel:instancing"),
    OFF("flywheel:off");

    private final String flywheelValue;

    FlywheelBackendMode(String flywheelValue) {
        this.flywheelValue = flywheelValue;
    }

    public String flywheelValue() {
        return flywheelValue;
    }

    public String translationKey() {
        return "enum.create_gmf.flywheel_backend." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
