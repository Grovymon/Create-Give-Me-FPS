package dev.creategmf.mixin.client;

import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import com.simibubi.create.content.contraptions.render.ActorVisual;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.BlockAndTintGetter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "com.simibubi.create.content.contraptions.actors.harvester.HarvesterActorVisual",
        "com.simibubi.create.content.contraptions.actors.roller.RollerActorVisual"
}, remap = false)
public abstract class HarvesterActorVisualMixin extends ActorVisual {
    @Unique
    private long createGmf$lastActorAnimationBucket = Long.MIN_VALUE;

    protected HarvesterActorVisualMixin(VisualizationContext visualizationContext, BlockAndTintGetter world,
            MovementContext context) {
        super(visualizationContext, world, context);
    }

    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$limitBladeFrames(CallbackInfo ci) {
        AnimationPolicy policy = DistantAnimationController.animationPolicy(context.position,
                MechanismAnimationGroup.HARVESTERS,
                Minecraft.getInstance().gameRenderer.getMainCamera());
        if (policy == AnimationPolicy.STATIC) {
            ci.cancel();
            return;
        }
        if (policy == AnimationPolicy.REDUCED) {
            long bucket = DistantAnimationController.currentAnimationBucket();
            if (bucket == createGmf$lastActorAnimationBucket) {
                ci.cancel();
            } else {
                createGmf$lastActorAnimationBucket = bucket;
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$limitBladeTicks(CallbackInfo ci) {
        AnimationPolicy policy = DistantAnimationController.animationPolicy(context.position,
                MechanismAnimationGroup.HARVESTERS,
                Minecraft.getInstance().gameRenderer.getMainCamera());
        if (policy == AnimationPolicy.STATIC) {
            ci.cancel();
        }
    }
}
