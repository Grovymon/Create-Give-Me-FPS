package dev.creategmf.mixin.client;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.api.instance.TickableInstance;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import dev.creategmf.optimization.animations.DistantAnimationController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InstanceManager.class, remap = false)
public abstract class LegacyInstanceManagerMixin {
    @Inject(method = "updateInstance", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$frame(DynamicInstance instance, float x, float y, float z, int bx, int by, int bz, CallbackInfo ci) {
        if (!DistantAnimationController.shouldUpdate(instance, Minecraft.getInstance().gameRenderer.getMainCamera())) ci.cancel();
    }
    @Inject(method = "tickInstance", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$tick(int x, int y, int z, TickableInstance instance, CallbackInfo ci) {
        if (!DistantAnimationController.shouldTick(instance)) ci.cancel();
    }
}
