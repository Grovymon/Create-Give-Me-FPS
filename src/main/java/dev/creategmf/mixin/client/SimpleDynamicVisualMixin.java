package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.lib.task.ConditionalPlan;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SimpleDynamicVisual.class, remap = false)
public interface SimpleDynamicVisualMixin {
    @ModifyReturnValue(method = "planFrame", at = @At("RETURN"), require = 0)
    private Plan<DynamicVisual.Context> createGmf$limitDistantDynamicFrames(
            Plan<DynamicVisual.Context> original) {
        Object visual = this;
        return new ConditionalPlan<>(context -> DistantAnimationController.shouldUpdate(visual, context.camera()),
                original);
    }
}
