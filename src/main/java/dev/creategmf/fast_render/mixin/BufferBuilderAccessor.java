package dev.creategmf.mixin.fast_render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Accesses the active vertex format without widening Minecraft classes globally. */
@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("format")
    VertexFormat create_gmf$getFormat();
}
