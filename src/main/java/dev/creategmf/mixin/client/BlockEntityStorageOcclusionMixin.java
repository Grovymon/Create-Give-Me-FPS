package dev.creategmf.mixin.client;

import dev.creategmf.optimization.occlusion.CreateOcclusionController;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.impl.visualization.storage.BlockEntityStorage;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents a known-hidden Create visual from being created until it is visible again. */
@Mixin(value = BlockEntityStorage.class, remap = false)
public abstract class BlockEntityStorageOcclusionMixin {
    @Inject(method = "willAccept", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$rejectCulledAdd(BlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (CreateOcclusionController.shouldRejectVisualCreation(blockEntity)) cir.setReturnValue(false);
    }

    @Inject(method = "createRaw", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$skipCulledCreate(VisualizationContext context, BlockEntity blockEntity, float partialTick,
            CallbackInfoReturnable<BlockEntityVisual<?>> cir) {
        if (CreateOcclusionController.shouldRejectVisualCreation(blockEntity)) cir.setReturnValue(null);
    }
}
