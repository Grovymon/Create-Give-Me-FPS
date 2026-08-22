package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.render.ContraptionVisual;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.diagnostics.GmfRuntimeStatus;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;
import dev.engine_room.flywheel.api.task.Plan;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.TickableVisual;
import dev.engine_room.flywheel.lib.task.ConditionalPlan;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Contraptions are entity visuals, so they do not pass through the block-entity
 * visual hooks.  A large windmill can therefore keep updating every frame even
 * when the Bearings and Contraptions group is disabled or made distant-static.
 */
@Mixin(value = ContraptionVisual.class, remap = false)
public abstract class ContraptionVisualMixin {
    @Shadow @Final protected AbstractContraptionEntity entity;
    @Unique private long createGmf$lastReducedFrameBucket = Long.MIN_VALUE;

    @ModifyReturnValue(method = "planFrame", at = @At("RETURN"), require = 1)
    private Plan<DynamicVisual.Context> createGmf$limitContraptionFrames(Plan<DynamicVisual.Context> original) {
        return new ConditionalPlan<>(context -> createGmf$shouldUpdateFrame(context), original);
    }

    @ModifyReturnValue(method = "planTick", at = @At("RETURN"), require = 1)
    private Plan<TickableVisual.Context> createGmf$limitContraptionTicks(Plan<TickableVisual.Context> original) {
        return new ConditionalPlan<>(context -> createGmf$shouldUpdateTick(), original);
    }

    private boolean createGmf$shouldUpdateFrame(DynamicVisual.Context context) {
        GmfRuntimeStatus.markAnimationHook();
        AnimationPolicy policy = DistantAnimationController.animationPolicy(entity.position(),
                MechanismAnimationGroup.BEARINGS_AND_CONTRAPTIONS, context.camera());
        if (policy == AnimationPolicy.STATIC) {
            return false;
        }
        if (policy == AnimationPolicy.FULL) {
            return true;
        }
        long bucket = DistantAnimationController.currentAnimationBucket();
        if (bucket == createGmf$lastReducedFrameBucket) {
            return false;
        }
        createGmf$lastReducedFrameBucket = bucket;
        return true;
    }

    private boolean createGmf$shouldUpdateTick() {
        GmfRuntimeStatus.markAnimationHook();
        AnimationPolicy policy = DistantAnimationController.animationPolicy(entity.position(),
                MechanismAnimationGroup.BEARINGS_AND_CONTRAPTIONS,
                Minecraft.getInstance().gameRenderer.getMainCamera());
        if (policy == AnimationPolicy.STATIC) {
            return false;
        }
        return policy == AnimationPolicy.FULL
                || DistantAnimationController.shouldUpdateReducedTick(entity.getId());
    }
}
