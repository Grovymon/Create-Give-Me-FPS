package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.SharedFluidAnimationRegistry;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpriteContents.class)
public abstract class SpriteContentsMixin {
    @Inject(method = "createTicker", at = @At("RETURN"), require = 0)
    private void createGmf$identifySharedFluidTicker(CallbackInfoReturnable<SpriteTicker> cir) {
        SpriteTicker ticker = cir.getReturnValue();
        if (ticker != null) {
            SharedFluidAnimationRegistry.register((SpriteContents) (Object) this, ticker);
        }
    }
}
