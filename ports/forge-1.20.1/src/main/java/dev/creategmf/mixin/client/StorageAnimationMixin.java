package dev.creategmf.mixin.client;

import java.util.List;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.impl.ImplDebugFlags;
import dev.engine_room.flywheel.impl.visualization.storage.LightUpdatedVisualStorage;
import dev.engine_room.flywheel.impl.visualization.storage.Storage;
import dev.engine_room.flywheel.lib.task.ConditionalPlan;
import dev.engine_room.flywheel.lib.task.ForEachPlan;
import dev.engine_room.flywheel.lib.task.NestedPlan;
import dev.engine_room.flywheel.lib.task.PlanMap;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import dev.engine_room.flywheel.lib.visual.SimpleTickableVisual;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Flywheel 1.0.5's actual dispatch path; keeps light and non-Create updates. */
@Mixin(value = Storage.class, remap = false)
public abstract class StorageAnimationMixin {
    @Shadow @Final protected PlanMap<DynamicVisual, DynamicVisual.Context> dynamicVisuals;
    @Shadow @Final protected PlanMap<TickableVisual, TickableVisual.Context> tickableVisuals;
    @Shadow @Final protected List<SimpleDynamicVisual> simpleDynamicVisuals;
    @Shadow @Final protected List<SimpleTickableVisual> simpleTickableVisuals;
    @Shadow @Final protected LightUpdatedVisualStorage lightUpdatedVisuals;

    @Inject(method = "framePlan", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$framePlan(CallbackInfoReturnable<Plan<DynamicVisual.Context>> cir) {
        Plan<DynamicVisual.Context> update = ConditionalPlan.<DynamicVisual.Context>on(() -> !ImplDebugFlags.PAUSE_UPDATES)
                .then(NestedPlan.of(dynamicVisuals,
                        ForEachPlan.of(() -> simpleDynamicVisuals, (visual, context) -> {
                            if (DistantAnimationController.shouldUpdate(visual, context.camera())) visual.beginFrame(context);
                        })));
        cir.setReturnValue(NestedPlan.of(lightUpdatedVisuals.plan(), update));
    }

    @Inject(method = "tickPlan", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$tickPlan(CallbackInfoReturnable<Plan<TickableVisual.Context>> cir) {
        cir.setReturnValue(ConditionalPlan.<TickableVisual.Context>on(() -> !ImplDebugFlags.PAUSE_UPDATES)
                .then(NestedPlan.of(tickableVisuals,
                        ForEachPlan.of(() -> simpleTickableVisuals, (visual, context) -> {
                            if (DistantAnimationController.shouldTick(visual)) visual.tick(context);
                        }))));
    }
}
