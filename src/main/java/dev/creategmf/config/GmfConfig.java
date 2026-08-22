package dev.creategmf.config;

import java.util.EnumMap;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GmfConfig {
    public static final int CURRENT_VERSION = 12;
    public static final ClientValues CLIENT;
    public static final ModConfigSpec SPEC;

    static {
        var pair = new ModConfigSpec.Builder().configure(ClientValues::new);
        CLIENT = pair.getLeft();
        SPEC = pair.getRight();
    }

    private GmfConfig() {
    }

    public static void save() {
        CLIENT.configVersion.set(CURRENT_VERSION);
        SPEC.save();
    }

    public static void resetAll() {
        CLIENT.enabled.set(true);
        CLIENT.showOverlay.set(false);
        CLIENT.profilerMode.set(ProfilerMode.LIGHT);
        CLIENT.flywheelBackend.set(FlywheelBackendMode.DEFAULT);
        CLIENT.acceleratedRenderer.set(true);
        CLIENT.createOcclusionCulling.set(true);
        CLIENT.diagnosticDurationSeconds.set(10);
        CLIENT.developerMode.set(false);
        CLIENT.developerLogging.set(true);
        CLIENT.automaticSpikeDetection.set(true);
        CLIENT.developerLogLevel.set(DeveloperLogLevel.NORMAL);
        applyProfile(PcProfile.MEDIUM);
        save();
    }

    public static void applyProfile(PcProfile profile) {
        if (profile == PcProfile.CUSTOM) {
            CLIENT.pcProfile.set(profile);
            return;
        }

        CLIENT.enabled.set(true);
        CLIENT.pcProfile.set(profile);
        CLIENT.beltItemShadowOptimization.set(true);
        CLIENT.renderTransportedBeltItems.set(profile != PcProfile.POTATO);
        CLIENT.filterCreateParticles.set(true);
        CLIENT.filterFluidParticles.set(true);
        CLIENT.renderSteamSmokeParticles.set(profile != PcProfile.POTATO && profile != PcProfile.LOW);
        CLIENT.renderSparkParticles.set(profile != PcProfile.POTATO && profile != PcProfile.LOW);
        CLIENT.renderItemBreakParticles.set(profile != PcProfile.POTATO && profile != PcProfile.LOW);
        CLIENT.renderWaterSplashParticles.set(profile != PcProfile.POTATO && profile != PcProfile.LOW);
        CLIENT.crushingOutputRendering.set(profile != PcProfile.POTATO && profile != PcProfile.LOW);
        // Entity Culling is a required client dependency. This safe visual-only
        // optimisation stays enabled for every preset, including Potato.
        CLIENT.createOcclusionCulling.set(true);
        CLIENT.mechanismAnimations.values().forEach(value -> value.set(profile != PcProfile.POTATO));

        switch (profile) {
            case POTATO -> applyRenderingValues(45, 0, 5, 0, CreateParticleMode.OFF, DistantAnimationMode.STATIC);
            case LOW -> applyRenderingValues(60, 16, 5, 0, CreateParticleMode.OFF, DistantAnimationMode.STATIC);
            case MEDIUM -> applyRenderingValues(75, 32, 10, 16, CreateParticleMode.REDUCED, DistantAnimationMode.REDUCED);
            case ABOVE_AVERAGE -> applyRenderingValues(90, 32, 15, 24, CreateParticleMode.REDUCED, DistantAnimationMode.REDUCED);
            case HIGH -> applyRenderingValues(120, 32, 20, 48, CreateParticleMode.FULL, DistantAnimationMode.REDUCED);
            case ULTRA -> applyRenderingValues(144, 32, 30, 128, CreateParticleMode.FULL, DistantAnimationMode.FULL);
            case CUSTOM -> throw new IllegalStateException("Handled above");
        }
    }

    private static void applyRenderingValues(int targetFps, double animationDistance, int animationFps,
            double shadowDistance, CreateParticleMode particles, DistantAnimationMode animationMode) {
        CLIENT.targetFps.set(targetFps);
        CLIENT.distantAnimationDistance.set(animationDistance);
        CLIENT.reducedAnimationFps.set(animationFps);
        CLIENT.animationUpdateTickDivisor.set(switch (animationMode) {
            case STATIC -> 5;
            case REDUCED -> animationFps <= 10 ? 3 : 2;
            case FULL -> 1;
        });
        CLIENT.beltItemShadowDistance.set(shadowDistance);
        CLIENT.createParticleMode.set(particles);
        CLIENT.distantAnimationMode.set(animationMode);
    }

    public static final class ClientValues {
        public final ModConfigSpec.IntValue configVersion;
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.IntValue targetFps;
        public final ModConfigSpec.EnumValue<PcProfile> pcProfile;
        public final ModConfigSpec.BooleanValue showOverlay;
        public final ModConfigSpec.EnumValue<ProfilerMode> profilerMode;
        public final ModConfigSpec.BooleanValue beltItemShadowOptimization;
        public final ModConfigSpec.BooleanValue renderTransportedBeltItems;
        public final ModConfigSpec.DoubleValue beltItemShadowDistance;
        public final ModConfigSpec.EnumValue<CreateParticleMode> createParticleMode;
        public final ModConfigSpec.BooleanValue filterCreateParticles;
        public final ModConfigSpec.BooleanValue filterFluidParticles;
        public final ModConfigSpec.BooleanValue renderSteamSmokeParticles;
        public final ModConfigSpec.BooleanValue renderSparkParticles;
        public final ModConfigSpec.BooleanValue renderItemBreakParticles;
        public final ModConfigSpec.BooleanValue renderWaterSplashParticles;
        public final ModConfigSpec.EnumValue<DistantAnimationMode> distantAnimationMode;
        public final ModConfigSpec.DoubleValue distantAnimationDistance;
        public final ModConfigSpec.IntValue reducedAnimationFps;
        public final ModConfigSpec.IntValue animationUpdateTickDivisor;
        public final ModConfigSpec.EnumValue<FlywheelBackendMode> flywheelBackend;
        public final ModConfigSpec.BooleanValue acceleratedRenderer;
        public final ModConfigSpec.BooleanValue crushingOutputRendering;
        public final ModConfigSpec.BooleanValue createOcclusionCulling;
        public final EnumMap<MechanismAnimationGroup, ModConfigSpec.BooleanValue> mechanismAnimations;
        public final ModConfigSpec.IntValue diagnosticDurationSeconds;
        public final ModConfigSpec.BooleanValue developerMode;
        public final ModConfigSpec.BooleanValue developerLogging;
        public final ModConfigSpec.BooleanValue automaticSpikeDetection;
        public final ModConfigSpec.EnumValue<DeveloperLogLevel> developerLogLevel;

        private ClientValues(ModConfigSpec.Builder builder) {
            configVersion = builder
                    .comment("GMF configuration schema. Managed by the mod.")
                    .defineInRange("configVersion", CURRENT_VERSION, 1, CURRENT_VERSION);

            builder.push("general");
            enabled = builder.define("enabled", true);
            targetFps = builder.defineInRange("targetFps", 60, 30, 360);
            pcProfile = builder.defineEnum("pcProfile", PcProfile.MEDIUM);
            showOverlay = builder.define("showPerformanceStatistics", false);
            builder.pop();

            builder.push("profiler");
            profilerMode = builder.defineEnum("mode", ProfilerMode.LIGHT);
            diagnosticDurationSeconds = builder.defineInRange("diagnosticDurationSeconds", 10, 5, 30);
            builder.pop();

            builder.push("developerMode");
            developerMode = builder.comment("Creates local GMF performance black-box reports while enabled.")
                    .define("enabled", false);
            developerLogging = builder.define("recordDiagnostics", true);
            automaticSpikeDetection = builder.define("automaticSpikeDetection", true);
            developerLogLevel = builder.comment("Only NORMAL is implemented; it uses low-overhead sampling.")
                    .defineEnum("logLevel", DeveloperLogLevel.NORMAL);
            builder.pop();

            builder.push("belts");
            beltItemShadowOptimization = builder.define("transportedItemShadowOptimization", true);
            beltItemShadowDistance = builder.defineInRange("transportedItemShadowDistance", 24.0, 0.0, 128.0);
            renderTransportedBeltItems = builder.define("renderTransportedItems", true);
            builder.pop();

            builder.push("createRendering");
            createParticleMode = builder.defineEnum("particleMode", CreateParticleMode.REDUCED);
            filterCreateParticles = builder.define("filterCreateParticles", true);
            filterFluidParticles = builder.define("filterFluidParticles", true);
            renderSteamSmokeParticles = builder.define("renderSteamAndSmokeParticles", true);
            renderSparkParticles = builder.define("renderSparkParticles", true);
            renderItemBreakParticles = builder.define("renderItemAndBlockBreakParticles", true);
            renderWaterSplashParticles = builder.define("renderWaterAndLavaSplashParticles", true);
            distantAnimationMode = builder.defineEnum("distantAnimationMode", DistantAnimationMode.REDUCED);
            distantAnimationDistance = builder.defineInRange("distantAnimationDistance", 32.0, 0.0, 32.0);
            reducedAnimationFps = builder.defineInRange("reducedAnimationFps", 10, 1, 30);
            animationUpdateTickDivisor = builder.comment(
                    "Updates client-side Create visuals every Nth tick. Does not affect machine simulation.")
                    .defineInRange("animationUpdateTickDivisor", 1, 1, 5);
            builder.pop();

            builder.push("flywheel");
            flywheelBackend = builder.defineEnum("backend", FlywheelBackendMode.DEFAULT);
            acceleratedRenderer = builder.comment(
                    "Use the CreateBetterFPS-derived SuperByteBuffer renderer when Sodium and Iris are installed. "
                            + "Restart the game after changing this option.")
                    .define("acceleratedRenderer", true);
            builder.pop();

            builder.push("crushingWheels");
            crushingOutputRendering = builder.define("renderLooseItemsNearCrushingWheels", true);
            builder.pop();

            builder.push("occlusionCulling");
            createOcclusionCulling = builder.comment(
                    "Uses the required Entity Culling visibility transitions to remove hidden Create block-entity visuals. "
                            + "Does not change machine logic. Disabled automatically when Create: Nowheel is installed.")
                    .define("createOcclusionCulling", true);
            builder.pop();

            builder.push("mechanismAnimations");
            mechanismAnimations = new EnumMap<>(MechanismAnimationGroup.class);
            for (MechanismAnimationGroup group : MechanismAnimationGroup.values()) {
                mechanismAnimations.put(group, builder.define(group.configKey(), true));
            }
            builder.pop();
        }
    }
}
