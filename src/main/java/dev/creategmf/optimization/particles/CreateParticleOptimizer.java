package dev.creategmf.optimization.particles;

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

    private CreateParticleOptimizer() {
    }

    public static boolean shouldCreate(ParticleOptions options) {
        if (!GmfConfig.CLIENT.enabled.get()) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        if (id == null || !"create".equals(id.getNamespace())) {
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
