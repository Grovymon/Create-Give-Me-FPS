package dev.creategmf.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

/** Exact visual settings only. Developer logging/profiler/session settings are not overwritten. */
public final class VisualConfigSnapshot {
    private final SettingsSnapshot<ConfigValue<?>> snapshot;
    private VisualConfigSnapshot(SettingsSnapshot<ConfigValue<?>> snapshot) { this.snapshot = snapshot; }
    public static VisualConfigSnapshot capture() {
        var c = GmfConfig.CLIENT;
        List<ConfigValue<?>> values = List.of(c.enabled, c.pcProfile, c.targetFps, c.beltItemShadowOptimization,
                c.beltItemShadowDistance, c.renderTransportedBeltItems, c.createParticleMode, c.filterCreateParticles,
                c.filterFluidParticles, c.renderSteamSmokeParticles, c.renderSparkParticles, c.renderItemBreakParticles,
                c.renderWaterSplashParticles, c.distantAnimationMode, c.distantAnimationDistance, c.reducedAnimationFps,
                c.animationUpdateTickDivisor, c.flywheelBackend, c.acceleratedRenderer, c.createOcclusionCulling,
                c.pauseOffScreenAnimations, c.disabledAddonMechanisms);
        Map<ConfigValue<?>, Object> copied = new LinkedHashMap<>();
        values.forEach(value -> copied.put(value, value.get()));
        c.mechanismAnimations.values().forEach(value -> copied.put(value, value.get()));
        return new VisualConfigSnapshot(new SettingsSnapshot<>(copied));
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void restore() { snapshot.restore((key, value) -> ((ConfigValue) key).set(value)); }
}
