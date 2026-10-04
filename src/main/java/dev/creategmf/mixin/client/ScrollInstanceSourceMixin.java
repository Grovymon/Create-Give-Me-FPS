package dev.creategmf.mixin.client;
import com.simibubi.create.content.processing.burner.ScrollInstance;
import dev.creategmf.optimization.animations.ScrollAnimationRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = ScrollInstance.class, remap = false)
public abstract class ScrollInstanceSourceMixin {
    @Inject(method = "speed(FF)Lcom/simibubi/create/content/processing/burner/ScrollInstance;",
            at = @At("RETURN"), require = 1)
    private void createGmf$sourceSpeed(float u, float v, CallbackInfoReturnable<ScrollInstance> cir) {
        ScrollAnimationRegistry.sourceSpeed(cir.getReturnValue(), u, v);
    }
    @Inject(method = "offset(FF)Lcom/simibubi/create/content/processing/burner/ScrollInstance;",
            at = @At("RETURN"), require = 1)
    private void createGmf$sourceOffset(float u, float v, CallbackInfoReturnable<ScrollInstance> cir) {
        ScrollAnimationRegistry.sourceOffset(cir.getReturnValue(), u, v);
    }
}
