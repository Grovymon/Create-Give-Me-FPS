package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.BlockEntityVisualAccess;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;

import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public abstract class AbstractBlockEntityVisualMixin implements BlockEntityVisualAccess {
    @Shadow
    @Final
    protected BlockPos pos;

    @Override
    public BlockPos createGmf$getWorldPosition() {
        return pos;
    }
}
