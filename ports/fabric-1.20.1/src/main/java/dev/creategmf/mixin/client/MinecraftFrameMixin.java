package dev.creategmf.mixin.client;

import dev.creategmf.client.ClientRuntime;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftFrameMixin {
    @Inject(method = "runTick", at = @At("HEAD"), require = 1)
    private void createGmf$frame(boolean renderLevel, CallbackInfo ci) {
        ClientRuntime.onRenderFrame();
    }
}
