package dev.creategmf.mixin.client;

import com.jozufozu.flywheel.api.InstanceData;
import com.simibubi.create.content.kinetics.base.flwdata.KineticData;
import dev.creategmf.optimization.animations.RotationAnimationRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InstanceData.class, remap = false)
public abstract class LegacyDataUploadMixin {
    @Inject(method = "checkDirtyAndClear", at = @At("HEAD"), require = 1)
    private void createGmf$applyShaderPolicy(CallbackInfoReturnable<Boolean> cir) {
        if ((Object)this instanceof KineticData data) RotationAnimationRegistry.beforeUpload(data);
    }
}
