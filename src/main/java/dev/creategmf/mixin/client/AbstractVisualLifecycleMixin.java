package dev.creategmf.mixin.client;
import dev.creategmf.optimization.animations.BlockEntityVisualAccess;
import dev.creategmf.optimization.animations.VisualPopulation;
import dev.engine_room.flywheel.lib.visual.AbstractVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = AbstractVisual.class, remap = false)
public abstract class AbstractVisualLifecycleMixin {
    @Inject(method = "delete", at = @At("HEAD"), require = 1)
    private void createGmf$deleteVisual(CallbackInfo ci) {
        if ((Object) this instanceof BlockEntityVisualAccess access)
            VisualPopulation.remove(access.createGmf$getAnimationState());
    }
}
