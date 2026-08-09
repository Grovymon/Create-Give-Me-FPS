package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;

import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Samples the CPU-rendered moving chain texture, which is not a Flywheel visual. */
@Mixin(value = ChainConveyorRenderer.class, remap = false)
public abstract class ChainConveyorRendererMixin {
    @ModifyExpressionValue(method = "renderChains", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/AnimationTickHolder;getRenderTime(Lnet/minecraft/world/level/LevelAccessor;)F"),
            require = 0)
    private float createGmf$sampleChainTime(float original, ChainConveyorBlockEntity blockEntity) {
        BlockPos position = blockEntity.getBlockPos();
        return DistantAnimationController.sampledRenderTime(original, position,
                MechanismAnimationGroup.CHAINS_AND_CONVEYORS);
    }

    /**
     * Final safeguard for the actual scrolling chain texture. Some Create add-ons
     * invoke renderChain through an altered renderer path, bypassing renderChains.
     */
    @ModifyVariable(method = "renderChain", at = @At("HEAD"), argsOnly = true, index = 2, require = 1)
    private static float createGmf$freezeChainTexture(float animation) {
        if (DistantAnimationController.shouldSuppressUnpositioned(
                MechanismAnimationGroup.CHAINS_AND_CONVEYORS)) {
            return 0;
        }
        return animation;
    }
}
