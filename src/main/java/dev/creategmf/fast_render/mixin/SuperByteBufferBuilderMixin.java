package dev.creategmf.mixin.fast_render;

// Adapted from CreateBetterFPS by MoePus (MIT). See THIRD_PARTY_NOTICES.md.
import dev.creategmf.config.GmfConfig;
import dev.creategmf.fast_render.SodiumByteBuffer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.createmod.catnip.render.MutableTemplateMesh;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.render.SuperByteBufferBuilder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SuperByteBufferBuilder.class, remap = false)
public abstract class SuperByteBufferBuilderMixin {
    @Shadow
    @Final
    protected MutableTemplateMesh mesh;

    @Shadow
    @Final
    protected IntList shadeSwapVertices;

    @Inject(method = "build", at = @At(value = "HEAD"), cancellable = true, require = 0)
    public void onBuild(CallbackInfoReturnable<SuperByteBuffer> cir) {
        if (!GmfConfig.CLIENT.acceleratedRenderer.get()) return;
        cir.setReturnValue(new SodiumByteBuffer(this.mesh.toImmutable(), this.shadeSwapVertices.toIntArray()));
    }
}
