package dev.creategmf.mixin.client;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRenderer;

import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Samples the CPU-rendered moving chain texture, which is not a Flywheel visual. */
@Mixin(value = ChainConveyorRenderer.class, remap = false)
public abstract class ChainConveyorRendererMixin {
    @ModifyVariable(method = "renderChains", at = @At(value = "STORE", ordinal = 0), ordinal = 0, require = 1)
    private float createGmf$sampleChainTexture(float animation, ChainConveyorBlockEntity blockEntity) {
        return DistantAnimationController.sampledRenderTime(animation, blockEntity.getBlockPos(),
                MechanismAnimationGroup.CHAINS_AND_CONVEYORS);
    }

    /**
     * Package positions are advanced by the conveyor simulation, rather than
     * by the scrolling texture time. A static chain must therefore skip that
     * moving overlay instead of showing packages glide along a frozen chain.
     */
    @Inject(method = "renderBox", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$skipMovingPackagesWhenStatic(ChainConveyorBlockEntity blockEntity,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight,
            net.minecraft.core.BlockPos origin,
            com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorPackage box,
            float partialTicks, CallbackInfo ci) {
        if (DistantAnimationController.animationPolicy(blockEntity.getBlockPos(),
                MechanismAnimationGroup.CHAINS_AND_CONVEYORS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC) {
            ci.cancel();
        }
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
