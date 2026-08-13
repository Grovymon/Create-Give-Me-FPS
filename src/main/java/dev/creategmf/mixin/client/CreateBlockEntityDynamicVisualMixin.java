package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Direct guard for Create visuals whose transforms are rewritten every frame. */
@Mixin(targets = {
        "com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorVisual",
        "com.simibubi.create.content.kinetics.crank.HandCrankVisual",
        "com.simibubi.create.content.kinetics.crank.ValveHandleVisual",
        "com.simibubi.create.content.kinetics.deployer.DeployerVisual",
        "com.simibubi.create.content.kinetics.flywheel.FlywheelVisual",
        "com.simibubi.create.content.kinetics.gauge.GaugeVisual",
        "com.simibubi.create.content.kinetics.mechanicalArm.ArmVisual",
        "com.simibubi.create.content.kinetics.mixer.MixerVisual",
        "com.simibubi.create.content.kinetics.press.PressVisual",
        "com.simibubi.create.content.kinetics.steamEngine.SteamEngineVisual"
        , "com.simibubi.create.content.logistics.tunnel.BeltTunnelVisual"
        , "com.simibubi.create.content.logistics.funnel.FunnelVisual"
        , "com.simibubi.create.content.logistics.depot.EjectorVisual"
        , "com.simibubi.create.content.logistics.packagePort.frogport.FrogportVisual"
        , "com.simibubi.create.content.logistics.packager.PackagerVisual"
        , "com.simibubi.create.content.contraptions.pulley.HosePulleyVisual"
        , "com.simibubi.create.content.processing.burner.BlazeBurnerVisual"
        , "com.simibubi.create.content.trains.signal.SignalVisual"
}, remap = false)
public abstract class CreateBlockEntityDynamicVisualMixin extends AbstractBlockEntityVisual<BlockEntity> {
    @Unique
    private long createGmf$lastAnimationBucket = Long.MIN_VALUE;

    protected CreateBlockEntityDynamicVisualMixin(VisualizationContext context, BlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
    }

    @Inject(method = "beginFrame", at = @At("HEAD"), cancellable = true, require = 0)
    private void createGmf$limitConcreteAnimation(DynamicVisual.Context context, CallbackInfo ci) {
        AnimationPolicy policy = DistantAnimationController.animationPolicy(
                pos, getClass().getName(), context.camera());
        if (policy == AnimationPolicy.STATIC) {
            ci.cancel();
            return;
        }
        if (policy == AnimationPolicy.REDUCED) {
            long bucket = DistantAnimationController.currentAnimationBucket();
            if (bucket == createGmf$lastAnimationBucket) {
                ci.cancel();
            } else {
                createGmf$lastAnimationBucket = bucket;
            }
        }
    }
}
