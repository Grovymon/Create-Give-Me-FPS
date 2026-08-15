package dev.creategmf.mixin.fast_render;

// Adapted from CreateBetterFPS by MoePus (MIT). See THIRD_PARTY_NOTICES.md.
import dev.creategmf.config.GmfConfig;
import dev.creategmf.fast_render.SodiumByteBuffer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.createmod.catnip.render.MutableTemplateMesh;
import net.createmod.catnip.render.SuperBufferFactory;
import net.createmod.catnip.render.SuperByteBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SuperBufferFactory.class, remap = false)
public abstract class SuperBufferFactoryMixin {
    @Inject(method = "create", at = @At(value = "HEAD"), cancellable = true, require = 0)
    public void onCreate(MeshData data, CallbackInfoReturnable<SuperByteBuffer> cir) {
        if (!GmfConfig.CLIENT.acceleratedRenderer.get()) return;
        cir.setReturnValue(new SodiumByteBuffer(new MutableTemplateMesh(data).toImmutable()));
    }
}
