package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.SharedFluidAnimationRegistry;

import net.minecraft.client.renderer.texture.SpriteTicker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.renderer.texture.SpriteContents$Ticker")
public abstract class SpriteContentsTickerMixin {
    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void createGmf$freezeSharedFluidFrame(int x, int y, CallbackInfo ci) {
        if (SharedFluidAnimationRegistry.shouldFreeze((SpriteTicker) (Object) this)) {
            ci.cancel();
        }
    }
}
