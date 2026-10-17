package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.SharedFluidAnimationRegistry;
import net.minecraft.client.renderer.texture.Tickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.TextureAtlasSprite$AnimatedTexture")
public abstract class LegacyTextureTickerMixin {
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$freezeFluid(CallbackInfo ci) {
        if (SharedFluidAnimationRegistry.shouldFreeze((Tickable)(Object)this)) ci.cancel();
    }
}
