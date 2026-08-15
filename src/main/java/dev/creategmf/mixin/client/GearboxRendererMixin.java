package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Gearboxes calculate their own fallback angle and bypass the base kinetic renderer. */
@Mixin(value = GearboxRenderer.class, remap = false)
public abstract class GearboxRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/AnimationTickHolder;getRenderTime(Lnet/minecraft/world/level/LevelAccessor;)F"),
            require = 1)
    private float createGmf$sampleGearboxTime(float original, GearboxBlockEntity gearbox) {
        return DistantAnimationController.sampledRenderTime(original, gearbox.getBlockPos(),
                MechanismAnimationGroup.SHAFTS_AND_COGS);
    }
}
