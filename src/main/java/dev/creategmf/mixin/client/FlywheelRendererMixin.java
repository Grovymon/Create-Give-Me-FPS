package dev.creategmf.mixin.client;

import com.simibubi.create.content.kinetics.flywheel.FlywheelBlockEntity;
import com.simibubi.create.content.kinetics.flywheel.FlywheelRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Flywheels use a separate visual-speed/angle path in Create's CPU fallback renderer. */
@Mixin(value = FlywheelRenderer.class, remap = false)
public abstract class FlywheelRendererMixin {
    @ModifyVariable(method = "renderFlywheel", at = @At("HEAD"), argsOnly = true, index = 5, require = 1)
    private float createGmf$freezeFlywheelAngle(float angle, FlywheelBlockEntity flywheel) {
        return DistantAnimationController.animationPolicy(flywheel.getBlockPos(),
                MechanismAnimationGroup.FLYWHEELS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC ? 0f : angle;
    }
}
