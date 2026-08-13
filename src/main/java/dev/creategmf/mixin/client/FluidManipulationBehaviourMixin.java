package dev.creategmf.mixin.client;

import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour;
import dev.creategmf.optimization.particles.CreateParticleOptimizer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the frequent hose fill/drain sound and its paired splash packet in an integrated client. */
@Mixin(value = FluidManipulationBehaviour.class, remap = false)
public abstract class FluidManipulationBehaviourMixin {
    @Inject(method = "playEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$skipFluidEffect(Level level, BlockPos position, Fluid fluid, boolean filling, CallbackInfo ci) {
        if (CreateParticleOptimizer.shouldSuppressFluidEffects()) {
            ci.cancel();
        }
    }
}
