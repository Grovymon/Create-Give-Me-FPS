package dev.creategmf.optimization.particles;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import dev.creategmf.config.CreateParticleMode;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.diagnostics.GmfRuntimeStatus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;

public final class CreateParticleOptimizer {
    private static final AtomicLong CREATE_PARTICLE_SEQUENCE = new AtomicLong();
    private static long requests;
    private static long allowed;
    private static long suppressed;
    private static long steamSmoke;
    private static long sparks;
    private static long itemBreak;
    private static long fluidSplash;
    private static long directFluidEffectsSuppressed;
    /** Name hints applied only after Create ownership has already been established. */
    private static final Set<String> FLUID_NAME_HINTS = Set.of(
            "bubble", "bubble_pop", "splash", "dripping_water", "falling_water",
            "landing_water", "underwater", "current_down", "bubble_column_up",
            "dripping_lava", "falling_lava", "landing_lava", "lava");

    private CreateParticleOptimizer() {
    }

    public static boolean shouldCreate(ParticleOptions options) {
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        // A vanilla identifier does NOT prove Create provenance. Vanilla water,
        // lava, smoke, block-breaking and item effects always bypass this global hook.
        if (id == null || !ParticleRules.registeredCreateParticle(id.getNamespace())) return true;
        GmfRuntimeStatus.markParticleHook();
        requests++;
        if (!GmfConfig.CLIENT.enabled.get()) { allowed++; return true; }
        Category category = category(id.getPath());
        countCategory(category);
        if (!isAllowedCategory(id)) {
            suppressed++;
            return false;
        }
        // "Off" is absolute: per-category switches must not accidentally let
        // fluid particles through again, as happened with hose pulley splashes.
        if (GmfConfig.CLIENT.createParticleMode.get() == CreateParticleMode.OFF) {
            suppressed++;
            return false;
        }
        if (!GmfConfig.CLIENT.filterCreateParticles.get()) {
            allowed++;
            return true;
        }
        boolean result = switch (GmfConfig.CLIENT.createParticleMode.get()) {
            case FULL -> true;
            case REDUCED -> CREATE_PARTICLE_SEQUENCE.getAndIncrement() % 3 == 0;
            case OFF -> false;
        };
        if (result) allowed++; else suppressed++;
        return result;
    }

    public static boolean shouldCreateVanillaEffect(BlockPos position, String ownerClassName) {
        if (!GmfConfig.CLIENT.enabled.get()) {
            return true;
        }
        // This method is called ONLY by the verified client Blaze Burner paths.
        if (!GmfConfig.CLIENT.renderSteamSmokeParticles.get()) return false;
        return switch (GmfConfig.CLIENT.createParticleMode.get()) {
            case FULL -> true;
            case REDUCED -> CREATE_PARTICLE_SEQUENCE.getAndIncrement() % 3 == 0;
            case OFF -> false;
        };
    }

    public static boolean shouldSuppressFluidEffects() {
        return GmfConfig.CLIENT.enabled.get()
                && (GmfConfig.CLIENT.createParticleMode.get() == CreateParticleMode.OFF
                || !GmfConfig.CLIENT.renderWaterSplashParticles.get());
    }

    /**
     * Marks one of Create's direct FluidFX calls as discarded.  These calls do
     * not always traverse the ordinary particle factory (notably the 20 block
     * particles emitted by a hose pulley splash), so keeping this counter
     * separate makes the developer report prove that the early hook fired.
     */
    public static boolean suppressDirectFluidEffect() {
        if (!shouldSuppressFluidEffects()) return false;
        GmfRuntimeStatus.markParticleHook();
        directFluidEffectsSuppressed++;
        return true;
    }

    /** Applied only after provenance has been established (Create namespace or explicit client callsite). */
    private static boolean isAllowedCategory(ResourceLocation id) {
        if (!GmfConfig.CLIENT.enabled.get()) return true;
        String path = id.getPath();
        if (isFluid(path)) return GmfConfig.CLIENT.renderWaterSplashParticles.get();
        if (path.contains("steam") || path.contains("smoke")) return GmfConfig.CLIENT.renderSteamSmokeParticles.get();
        if (path.contains("spark") || path.contains("electric")) return GmfConfig.CLIENT.renderSparkParticles.get();
        if (path.contains("block") || path.contains("terrain") || path.contains("item") || path.contains("poof")) {
            return GmfConfig.CLIENT.renderItemBreakParticles.get();
        }
        return true;
    }

    private static boolean isFluid(String path) {
        return FLUID_NAME_HINTS.contains(path)
                || path.contains("splash") || path.contains("water") || path.contains("lava") || path.contains("bubble");
    }

    public static ParticleCounters counters() {
        return new ParticleCounters(requests, allowed, suppressed, steamSmoke, sparks, itemBreak, fluidSplash,
                directFluidEffectsSuppressed);
    }

    private static Category category(String path) {
        if (isFluid(path)) return Category.FLUID_SPLASH;
        if (path.contains("steam") || path.contains("smoke")) return Category.STEAM_SMOKE;
        if (path.contains("spark") || path.contains("electric")) return Category.SPARKS;
        if (path.contains("block") || path.contains("terrain") || path.contains("item") || path.contains("poof")) {
            return Category.ITEM_BREAK;
        }
        return Category.OTHER;
    }

    private static void countCategory(Category category) {
        switch (category) {
            case STEAM_SMOKE -> steamSmoke++;
            case SPARKS -> sparks++;
            case ITEM_BREAK -> itemBreak++;
            case FLUID_SPLASH -> fluidSplash++;
            case OTHER -> { }
        }
    }

    private enum Category { STEAM_SMOKE, SPARKS, ITEM_BREAK, FLUID_SPLASH, OTHER }

    public record ParticleCounters(long requests, long allowed, long suppressed, long steamSmoke, long sparks,
            long itemBreak, long fluidSplash, long directFluidEffectsSuppressed) {
    }
}
