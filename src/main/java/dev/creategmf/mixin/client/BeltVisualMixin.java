package dev.creategmf.mixin.client;

import com.simibubi.create.content.kinetics.belt.BeltVisual;
import com.simibubi.create.content.processing.burner.ScrollInstance;

import dev.creategmf.optimization.animations.BlockEntityVisualAccess;
import dev.creategmf.optimization.animations.ScrollAnimationRegistry;
import dev.creategmf.config.MechanismAnimationGroup;

import net.createmod.catnip.render.SpriteShiftEntry;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BeltVisual.class, remap = false)
public abstract class BeltVisualMixin {
    @Inject(method = "setup", at = @At("HEAD"), require = 0)
    private void createGmf$freezeDistantBelt(ScrollInstance instance, boolean bottom, SpriteShiftEntry spriteShift,
            CallbackInfoReturnable<ScrollInstance> cir) {
        ScrollAnimationRegistry.prepare(instance,
                ((BlockEntityVisualAccess) this).createGmf$getWorldPosition(), MechanismAnimationGroup.BELTS);
    }
}
