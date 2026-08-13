package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity;
import com.simibubi.create.content.kinetics.fan.EncasedFanRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The fan blades have an independent CPU render-time path. */
@Mixin(value = EncasedFanRenderer.class, remap = false)
public abstract class EncasedFanRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/AnimationTickHolder;getRenderTime(Lnet/minecraft/world/level/LevelAccessor;)F"),
            require = 1)
    private float createGmf$sampleFanTime(float original, EncasedFanBlockEntity fan) {
        return DistantAnimationController.sampledRenderTime(original, fan.getBlockPos(),
                MechanismAnimationGroup.FANS);
    }
}
