package dev.creategmf.mixin.client;

import com.simibubi.create.content.trains.display.FlapDisplaySection;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = FlapDisplaySection.class, remap = false)
public abstract class FlapDisplaySectionMixin {
    @ModifyVariable(method = "refresh", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private boolean createGmf$makeTextChangesInstant(boolean transition) {
        return DistantAnimationController.shouldSuppressUnpositioned(MechanismAnimationGroup.OTHER)
                ? false : transition;
    }

    @ModifyVariable(method = "tick", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private boolean createGmf$finishExistingFlapAnimation(boolean instant) {
        return instant || DistantAnimationController.shouldSuppressUnpositioned(MechanismAnimationGroup.OTHER);
    }
}
