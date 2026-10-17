package dev.creategmf.mixin.client;

import com.simibubi.create.content.kinetics.base.flwdata.KineticData;
import dev.creategmf.legacy.KineticDataAccess;
import dev.creategmf.optimization.animations.RotationAnimationRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = KineticData.class, remap = false)
public abstract class LegacyKineticDataMixin implements KineticDataAccess {
    @Shadow float rotationalSpeed;
    @Shadow float rotationOffset;
    public float createGmf$getSpeed() { return rotationalSpeed; }
    public float createGmf$getOffset() { return rotationOffset; }
    public void createGmf$setSpeed(float value) { rotationalSpeed = value; }
    public void createGmf$setOffset(float value) { rotationOffset = value; }
    @Inject(method = "setRotationalSpeed", at = @At("RETURN"), require = 1)
    private void createGmf$recordSpeed(float value, CallbackInfoReturnable<KineticData> cir) {
        RotationAnimationRegistry.record((KineticData)(Object)this, value, null);
    }
    @Inject(method = "setRotationOffset", at = @At("RETURN"), require = 1)
    private void createGmf$recordOffset(float value, CallbackInfoReturnable<KineticData> cir) {
        RotationAnimationRegistry.record((KineticData)(Object)this, null, value);
    }
}
