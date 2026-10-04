package dev.creategmf.mixin.client;

import com.simibubi.create.content.processing.burner.ScrollInstance;
import com.simibubi.create.content.kinetics.base.RotatingInstance;

import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.animations.RotationAnimationRegistry;
import dev.creategmf.optimization.animations.ScrollAnimationRegistry;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractInstance.class, remap = false)
public abstract class AbstractInstanceMixin {
    @Inject(method = "delete", at = @At("HEAD"), require = 1)
    private void createGmf$forgetDeletedInstance(CallbackInfo ci) {
        if ((Object) this instanceof RotatingInstance rotating) RotationAnimationRegistry.forget(rotating);
        if ((Object) this instanceof ScrollInstance scroll) ScrollAnimationRegistry.forget(scroll);
    }
    @Inject(method = "setChanged", at = @At("HEAD"), require = 0)
    private void createGmf$freezeUnclassifiedScrolling(CallbackInfo ci) {
        if ((Object) this instanceof RotatingInstance rotating) {
            RotationAnimationRegistry.beforeUpload(rotating);
            return;
        }
        if (!((Object) this instanceof ScrollInstance scroll)) {
            return;
        }
        // Unknown ownership/render-origin coordinates are not world positions.
        // Never guess which addon, contraption or shared shader owns an instance.
        ScrollAnimationRegistry.beforeUpload(scroll);
    }
}
