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
    @Unique
    private boolean createGmf$scrollSuppressed;
    @Unique
    private float createGmf$savedSpeedU;
    @Unique
    private float createGmf$savedSpeedV;

    @Inject(method = "setChanged", at = @At("HEAD"), require = 0)
    private void createGmf$freezeUnclassifiedScrolling(CallbackInfo ci) {
        if ((Object) this instanceof RotatingInstance rotating) {
            RotationAnimationRegistry.beforeUpload(rotating);
            return;
        }
        if (!((Object) this instanceof ScrollInstance scroll)) {
            return;
        }
        if (ScrollAnimationRegistry.beforeUpload(scroll)) {
            return;
        }
        boolean suppress = DistantAnimationController.shouldFreezeUnclassifiedScrolling(
                scroll.x, scroll.y, scroll.z);
        if (suppress) {
            if (!createGmf$scrollSuppressed || scroll.speedU != 0 || scroll.speedV != 0) {
                createGmf$savedSpeedU = scroll.speedU;
                createGmf$savedSpeedV = scroll.speedV;
            }
            scroll.speedU = 0;
            scroll.speedV = 0;
            createGmf$scrollSuppressed = true;
        } else if (createGmf$scrollSuppressed) {
            scroll.speedU = createGmf$savedSpeedU;
            scroll.speedV = createGmf$savedSpeedV;
            createGmf$scrollSuppressed = false;
        }
    }
}
