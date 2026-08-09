package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;

import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.lib.task.ConditionalPlan;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SimpleTickableVisual.class, remap = false)
public interface SimpleTickableVisualMixin {
    @ModifyReturnValue(method = "planTick", at = @At("RETURN"), require = 0)
    private Plan<TickableVisual.Context> createGmf$limitDistantVisualTicks(
            Plan<TickableVisual.Context> original) {
        Object visual = this;
        return new ConditionalPlan<>(context -> DistantAnimationController.shouldTick(visual), original);
    }
}
