package dev.creategmf.mixin.client;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;

import dev.creategmf.optimization.particles.CreateParticleOptimizer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BlazeBurnerBlockEntity.class, remap = false)
public abstract class BlazeBurnerBlockEntityMixin {
    @Inject(method = "spawnParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$limitAmbientParticles(BlazeBurnerBlock.HeatLevel heatLevel, double burstMultiplier,
            CallbackInfo ci) {
        BlazeBurnerBlockEntity burner = (BlazeBurnerBlockEntity) (Object) this;
        if (!CreateParticleOptimizer.shouldCreateVanillaEffect(burner.getBlockPos(), burner.getClass().getName())) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnParticleBurst", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$limitParticleBurst(boolean soulFlame, CallbackInfo ci) {
        BlazeBurnerBlockEntity burner = (BlazeBurnerBlockEntity) (Object) this;
        if (!CreateParticleOptimizer.shouldCreateVanillaEffect(burner.getBlockPos(), burner.getClass().getName())) {
            ci.cancel();
        }
    }
}
