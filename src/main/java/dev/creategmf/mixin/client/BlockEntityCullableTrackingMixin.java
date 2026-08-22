package dev.creategmf.mixin.client;

import dev.creategmf.optimization.occlusion.CreateOcclusionController;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity Culling adds {@code setCulled} to BlockEntity at runtime. Entity
 * Culling is a required client dependency; {@code require=0} still keeps this
 * mixin tolerant of method-name changes in a compatible release.
 */
@SuppressWarnings({"MixinAnnotationTarget", "UnresolvedMixinReference"})
@Mixin(value = BlockEntity.class, priority = 1090)
public abstract class BlockEntityCullableTrackingMixin {
    @Unique private boolean createGmf$transitionPending;
    @Unique private boolean createGmf$transitionValue;

    @Inject(method = "setCulled(Z)V", at = @At("HEAD"), remap = false, require = 0)
    private void createGmf$captureCullTransition(boolean value, CallbackInfo ci) {
        createGmf$transitionPending = true;
        createGmf$transitionValue = value;
    }

    @Inject(method = "setCulled(Z)V", at = @At("TAIL"), remap = false, require = 0)
    private void createGmf$applyCullTransition(boolean value, CallbackInfo ci) {
        if (!createGmf$transitionPending || createGmf$transitionValue != value) return;
        createGmf$transitionPending = false;
        CreateOcclusionController.onCullChanged((BlockEntity) (Object) this, value);
    }

    @Inject(method = "setRemoved", at = @At("TAIL"), require = 0)
    private void createGmf$forgetRemovedBlockEntity(CallbackInfo ci) {
        CreateOcclusionController.forget((BlockEntity) (Object) this);
    }
}
