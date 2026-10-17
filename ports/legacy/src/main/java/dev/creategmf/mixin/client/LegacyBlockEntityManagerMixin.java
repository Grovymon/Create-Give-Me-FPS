package dev.creategmf.mixin.client;

import com.jozufozu.flywheel.backend.instancing.AbstractInstance;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstanceManager;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockEntityInstanceManager.class, remap = false)
public abstract class LegacyBlockEntityManagerMixin {
    @Inject(method = "canCreateInstance(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$accept(BlockEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (CreateOcclusionController.shouldRejectVisualCreation(entity)) cir.setReturnValue(false);
    }
    @Inject(method = "createRaw(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/jozufozu/flywheel/backend/instancing/AbstractInstance;", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$create(BlockEntity entity, CallbackInfoReturnable<AbstractInstance> cir) {
        if (CreateOcclusionController.shouldRejectVisualCreation(entity)) cir.setReturnValue(null);
    }
}
