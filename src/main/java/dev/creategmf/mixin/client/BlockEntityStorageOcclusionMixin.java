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
    @org.spongepowered.asm.mixin.Shadow public abstract BlockEntityVisual<?> visualAtPos(long pos);
    @Inject(method = "willAccept(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$rejectCulledAdd(BlockEntity blockEntity, CallbackInfoReturnable<Boolean> cir) {
        if (CreateOcclusionController.shouldRejectVisualCreation(blockEntity,
                visualAtPos(blockEntity.getBlockPos().asLong()) == null)) cir.setReturnValue(false);
    }

    @Inject(method = "createRaw(Ldev/engine_room/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Ldev/engine_room/flywheel/api/visual/BlockEntityVisual;",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$skipCulledCreate(VisualizationContext context, BlockEntity blockEntity, float partialTick,
            CallbackInfoReturnable<BlockEntityVisual<?>> cir) {
        if (CreateOcclusionController.shouldRejectVisualCreation(blockEntity)) cir.setReturnValue(null);
    }
}
