package dev.creategmf.mixin.client;

import dev.creategmf.optimization.particles.CreateParticleOptimizer;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleOptions;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
    @Inject(method = "createParticle", at = @At("HEAD"), cancellable = true)
    private void createGmf$limitCreateParticles(ParticleOptions options, double x, double y, double z,
            double velocityX, double velocityY, double velocityZ, CallbackInfoReturnable<Particle> cir) {
        if (!CreateParticleOptimizer.shouldCreate(options)) {
            cir.setReturnValue(null);
        }
    }
}
