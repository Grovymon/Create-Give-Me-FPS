package dev.creategmf.optimization.particles;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import dev.creategmf.config.CreateParticleMode;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.optimization.animations.DistantAnimationController;
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
    /** Hose pulleys emit these normal Minecraft fluid effects at the submerged end. */
    private static final Set<String> HEAVY_VANILLA_FLUID_PARTICLES = Set.of(
            "bubble", "bubble_pop", "splash", "dripping_water", "falling_water",
            "landing_water", "underwater", "current_down", "bubble_column_up",
            "dripping_lava", "falling_lava", "landing_lava", "lava");

    private CreateParticleOptimizer() {
    }

    public static boolean shouldCreate(ParticleOptions options) {
        GmfRuntimeStatus.markParticleHook();
        requests++;
        if (!GmfConfig.CLIENT.enabled.get()) {
            allowed++;
            return true;
        }
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (id == null) {
            allowed++;
            return true;
        }
        boolean createParticle = "create".equals(id.getNamespace());
        boolean fluidEffect = "minecraft".equals(id.getNamespace())
                && HEAVY_VANILLA_FLUID_PARTICLES.contains(id.getPath());
        Category category = category(id.getPath());
        countCategory(category);
        if (!isAllowedCategory(id)) {
            suppressed++;
            return false;
        }
        if (!createParticle && !fluidEffect) {
            allowed++;
            return true;
        }
        // "Off" is absolute: per-category switches must not accidentally let
        // fluid particles through again, as happened with hose pulley splashes.
        if (GmfConfig.CLIENT.createParticleMode.get() == CreateParticleMode.OFF) {
            suppressed++;
            return false;
        }
        if (createParticle && !GmfConfig.CLIENT.filterCreateParticles.get()) {
            allowed++;
            return true;
        }
        if (fluidEffect && !GmfConfig.CLIENT.filterFluidParticles.get()) {
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
        if (DistantAnimationController.shouldBeStatic(position, ownerClassName)) {
            return false;
        }
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
     * Categories are deliberately based on the registered particle identifier,
     * so they also cover vanilla effects triggered by Create (hose pulleys,
     * drills and saws) before the particles reach the client engine.
     */
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
        return HEAVY_VANILLA_FLUID_PARTICLES.contains(path)
                || path.contains("splash") || path.contains("water") || path.contains("lava") || path.contains("bubble");
    }

    public static ParticleCounters counters() {
        return new ParticleCounters(requests, allowed, suppressed, steamSmoke, sparks, itemBreak, fluidSplash);
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
            long itemBreak, long fluidSplash) {
    }
}
