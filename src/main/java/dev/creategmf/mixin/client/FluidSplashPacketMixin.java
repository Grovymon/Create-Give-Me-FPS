package dev.creategmf.mixin.client;

import com.simibubi.create.content.fluids.transfer.FluidSplashPacket;
import dev.creategmf.optimization.particles.CreateParticleOptimizer;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels splash packets before Create can create its vanilla block particles.
 * This is needed for worlds hosted elsewhere, where the server-side behaviour
 * mixin cannot suppress the packet before it reaches this client.
 */
@Mixin(value = FluidSplashPacket.class, remap = false)
public abstract class FluidSplashPacketMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$skipNetworkSplash(LocalPlayer player, CallbackInfo ci) {
        if (CreateParticleOptimizer.shouldSuppressFluidEffects()) {
            ci.cancel();
        }
    }
}
