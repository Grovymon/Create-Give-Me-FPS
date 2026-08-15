package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportBlockEntity;
import com.simibubi.create.content.logistics.packagePort.frogport.FrogportRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the Frogport model visible while preventing its fallback deploy animation. */
@Mixin(value = FrogportRenderer.class, remap = false)
public abstract class FrogportRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/packagePort/frogport/FrogportBlockEntity;isAnimationInProgress()Z"),
            require = 1)
    private boolean createGmf$freezeFrogport(boolean animating, FrogportBlockEntity frogport) {
        return DistantAnimationController.animationPolicy(frogport.getBlockPos(),
                MechanismAnimationGroup.PACKAGE_PORTS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC ? false : animating;
    }
}
