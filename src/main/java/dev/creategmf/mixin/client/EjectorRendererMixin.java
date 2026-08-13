package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.depot.EjectorBlockEntity;
import com.simibubi.create.content.logistics.depot.EjectorRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Weighted ejectors have their own lid transform and do not use a Flywheel visual when the backend is off. */
@Mixin(value = EjectorRenderer.class, remap = false)
public abstract class EjectorRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/depot/EjectorBlockEntity;getLidProgress(F)F"), require = 1)
    private float createGmf$freezeEjectorLid(float original, EjectorBlockEntity ejector) {
        return DistantAnimationController.animationPolicy(ejector.getBlockPos(),
                MechanismAnimationGroup.EJECTORS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC ? 0f : original;
    }
}
