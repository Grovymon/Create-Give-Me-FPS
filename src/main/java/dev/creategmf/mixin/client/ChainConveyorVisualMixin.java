package dev.creategmf.mixin.client;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual;

import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChainConveyorVisual.class, remap = false)
public abstract class ChainConveyorVisualMixin extends AbstractBlockEntityVisual<ChainConveyorBlockEntity> {
    protected ChainConveyorVisualMixin(VisualizationContext context, ChainConveyorBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$limitPackageAnimationTicks(TickableVisual.Context context, CallbackInfo ci) {
        if (!DistantAnimationController.shouldTick((Object) this)) {
            ci.cancel();
        }
    }
}
