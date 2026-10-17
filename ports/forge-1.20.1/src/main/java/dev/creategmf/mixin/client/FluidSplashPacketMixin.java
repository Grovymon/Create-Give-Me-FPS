package dev.creategmf.mixin.client;

import com.simibubi.create.content.fluids.transfer.FluidSplashPacket;
import dev.creategmf.optimization.particles.CreateParticleOptimizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Create 6.0.8 Forge's handle(Context) returns a handled flag. */
@Mixin(value = FluidSplashPacket.class, remap = false)
public abstract class FluidSplashPacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$skipNetworkSplash(CallbackInfoReturnable<Boolean> cir) {
        if (CreateParticleOptimizer.suppressDirectFluidEffect()) cir.setReturnValue(true);
    }
}
