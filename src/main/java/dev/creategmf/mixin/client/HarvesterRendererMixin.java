package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.contraptions.actors.harvester.HarvesterRenderer;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Covers stationary and fallback-rendered harvester/roller blades. */
@Mixin(value = HarvesterRenderer.class, remap = false)
public abstract class HarvesterRendererMixin {
    @ModifyExpressionValue(method = "transform", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/AnimationTickHolder;getRenderTime(Lnet/minecraft/world/level/Level;)F"),
            require = 0)
    private static float createGmf$sampleBladeTime(float original) {
        return DistantAnimationController.sampledRenderTime(original, null, MechanismAnimationGroup.HARVESTERS);
    }
}
