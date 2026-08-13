package dev.creategmf.mixin.client;

import com.simibubi.create.content.fluids.FluidFX;
import dev.creategmf.optimization.particles.CreateParticleOptimizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stops Create's direct fluid effects, including the hose pulley splash packet. */
@Mixin(value = FluidFX.class, remap = false)
public abstract class FluidFXMixin {
    @Inject(method = "splash", at = @At("HEAD"), cancellable = true, require = 0)
    private static void createGmf$skipSplash(BlockPos position, FluidStack fluid, CallbackInfo ci) {
        if (CreateParticleOptimizer.shouldSuppressFluidEffects()) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnRimParticles", at = @At("HEAD"), cancellable = true, require = 0)
    private static void createGmf$skipRimParticles(Level level, BlockPos position, Direction direction, int count,
            ParticleOptions particle, float spread, CallbackInfo ci) {
        if (CreateParticleOptimizer.shouldSuppressFluidEffects()) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnPouringLiquid", at = @At("HEAD"), cancellable = true, require = 0)
    private static void createGmf$skipPouringParticles(Level level, BlockPos position, int count,
            ParticleOptions particle, float spread, Vec3 direction, boolean inward, CallbackInfo ci) {
        if (CreateParticleOptimizer.shouldSuppressFluidEffects()) {
            ci.cancel();
        }
    }
}
