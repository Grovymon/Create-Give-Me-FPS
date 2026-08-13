package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.crank.HandCrankBlockEntity;
import com.simibubi.create.content.kinetics.crank.HandCrankRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The hand crank has an independent angle and does not call getAngleForBe. */
@Mixin(value = HandCrankRenderer.class, remap = false)
public abstract class HandCrankRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/kinetics/crank/HandCrankBlockEntity;getIndependentAngle(F)F"),
            require = 1)
    private float createGmf$freezeIndependentHandle(float original, HandCrankBlockEntity crank) {
        return DistantAnimationController.animationPolicy(crank.getBlockPos(),
                MechanismAnimationGroup.SHAFTS_AND_COGS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC ? 0f : original;
    }
}
