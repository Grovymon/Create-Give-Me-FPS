package dev.creategmf.optimization.particles;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import dev.creategmf.config.CreateParticleMode;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.optimization.animations.DistantAnimationController;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.resources.ResourceLocation;

public final class CreateParticleOptimizer {
    private static final AtomicLong CREATE_PARTICLE_SEQUENCE = new AtomicLong();
    /** Hose pulleys emit these normal Minecraft fluid effects at the submerged end. */
    private static final Set<String> HEAVY_VANILLA_FLUID_PARTICLES = Set.of(
            "bubble", "bubble_pop", "splash", "dripping_water", "falling_water",
            "landing_water", "underwater", "current_down", "bubble_column_up",
            "dripping_lava", "falling_lava", "landing_lava", "lava");

    private CreateParticleOptimizer() {
    }

    public static boolean shouldCreate(ParticleOptions options) {
        if (!GmfConfig.CLIENT.enabled.get()) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (id == null) {
            return true;
        }
        boolean createParticle = "create".equals(id.getNamespace());
        boolean fluidEffect = "minecraft".equals(id.getNamespace())
                && HEAVY_VANILLA_FLUID_PARTICLES.contains(id.getPath());
        if (!createParticle && !fluidEffect) {
            return true;
        }
        if (createParticle && !GmfConfig.CLIENT.filterCreateParticles.get()) {
            return true;
        }
        if (fluidEffect && !GmfConfig.CLIENT.filterFluidParticles.get()) {
            return true;
        }
        return switch (GmfConfig.CLIENT.createParticleMode.get()) {
            case FULL -> true;
            case REDUCED -> CREATE_PARTICLE_SEQUENCE.getAndIncrement() % 3 == 0;
            case OFF -> false;
        };
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
}
