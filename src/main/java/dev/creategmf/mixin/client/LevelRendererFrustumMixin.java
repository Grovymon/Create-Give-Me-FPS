package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.OffScreenAnimations;
import dev.creategmf.optimization.animations.RotationAnimationRegistry;
import dev.creategmf.optimization.animations.ScrollAnimationRegistry;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererFrustumMixin {
    @Inject(method = "prepareCullFrustum", at = @At("TAIL"), require = 1)
    private void createGmf$prepareCamera(CallbackInfo ci) {
        OffScreenAnimations.prepare(((LevelRenderer) (Object) this).getFrustum());
        // Reevaluate even within the same reduced bucket: turning back resumes this frame.
        RotationAnimationRegistry.onFrame();
        ScrollAnimationRegistry.onFrame();
    }
}
