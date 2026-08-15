package dev.creategmf.mixin.fast_render;

// Adapted from CreateBetterFPS by MoePus (MIT). See THIRD_PARTY_NOTICES.md.
import dev.creategmf.config.GmfConfig;
import dev.creategmf.fast_render.SodiumByteBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.createmod.catnip.render.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShadedBlockSbbBuilder.class, remap = false)
public abstract class ShadedBlockSbbBuilderMixin {
    @Shadow
    protected BufferBuilder bufferBuilder;

    @Shadow
    protected boolean invertFakeNormal;

    @Shadow
    @Final
    protected IntList shadeSwapVertices;


    @Inject(method = "end", at = @At(value = "HEAD"), cancellable = true, require = 0)
    public void onEnd(CallbackInfoReturnable<SuperByteBuffer> cir) {
        if (!GmfConfig.CLIENT.acceleratedRenderer.get()) return;
        MeshData data = bufferBuilder.build();
        TemplateMesh mesh;

        if (data != null) {
            mesh = new MutableTemplateMesh(data).toImmutable();
            data.close();
        } else {
            mesh = new TemplateMesh(0);
        }

        cir.setReturnValue(new SodiumByteBuffer(mesh, shadeSwapVertices.toIntArray(), invertFakeNormal));
    }
}
