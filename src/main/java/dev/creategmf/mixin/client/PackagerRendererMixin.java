package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.logistics.packager.PackagerBlockEntity;
import com.simibubi.create.content.logistics.packager.PackagerRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Packagers animate their tray and hatch without consulting the generic visual guard. */
@Mixin(value = PackagerRenderer.class, remap = false)
public abstract class PackagerRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lcom/simibubi/create/content/logistics/packager/PackagerBlockEntity;getTrayOffset(F)F"),
            require = 1)
    private float createGmf$freezePackagerTray(float original, PackagerBlockEntity packager) {
        return isStatic(packager) ? 0f : original;
    }

    @ModifyReturnValue(method = "isHatchOpen", at = @At("RETURN"), require = 1)
    private static boolean createGmf$closePackagerHatch(boolean original, PackagerBlockEntity packager) {
        return isStatic(packager) ? false : original;
    }

    private static boolean isStatic(PackagerBlockEntity packager) {
        return DistantAnimationController.animationPolicy(packager.getBlockPos(),
                MechanismAnimationGroup.PACKAGE_PORTS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC;
    }
}
