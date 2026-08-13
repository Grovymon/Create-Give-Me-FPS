package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelBlockEntity;
import com.simibubi.create.content.logistics.tunnel.BeltTunnelRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Freezes the flaps of brass belt tunnels in Create's CPU renderer. */
@Mixin(value = BeltTunnelRenderer.class, remap = false)
public abstract class BeltTunnelRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/LerpedFloat;getValue(F)F"), require = 1)
    private float createGmf$freezeTunnelFlaps(float original, BeltTunnelBlockEntity tunnel) {
        return DistantAnimationController.animationPolicy(tunnel.getBlockPos(),
                MechanismAnimationGroup.TUNNELS_AND_FUNNELS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC ? 0f : original;
    }
}
