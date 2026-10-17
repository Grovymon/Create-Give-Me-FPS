package dev.creategmf.mixin.client;

import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstance;
import dev.creategmf.optimization.animations.BlockEntityVisualAccess;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = BlockEntityInstance.class, remap = false)
public abstract class LegacyInstanceOwnerMixin implements BlockEntityVisualAccess {
    @Shadow @Final protected BlockPos pos;
    public BlockPos createGmf$getWorldPosition() { return pos; }
}
