package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.SharedFluidAnimationRegistry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.Tickable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TextureAtlasSprite.class)
public abstract class LegacyTextureSpriteMixin {
    @Inject(method = "getAnimationTicker", at = @At("RETURN"), require = 1)
    private void createGmf$fluidTicker(CallbackInfoReturnable<Tickable> cir) {
        SharedFluidAnimationRegistry.register(((TextureAtlasSprite)(Object)this).getName(), cir.getReturnValue());
    }
}
