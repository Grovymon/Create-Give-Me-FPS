package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import dev.creategmf.config.GmfConfig;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = KineticBlockEntityRenderer.class, remap = false)
public abstract class KineticBlockEntityRendererMixin {
    @ModifyReturnValue(method = "getAngleForBe", at = @At("RETURN"), require = 0)
    private static float createGmf$sampleFallbackRotation(float original, KineticBlockEntity blockEntity,
            BlockPos position, Axis axis) {
        AnimationPolicy policy = DistantAnimationController.animationPolicy(position,
                blockEntity.getClass().getName(), Minecraft.getInstance().gameRenderer.getMainCamera());
        if (policy == AnimationPolicy.FULL) {
            return original;
        }

        float offset = KineticBlockEntityRenderer.getRotationOffsetForPosition(blockEntity, position, axis);
        if (policy == AnimationPolicy.STATIC) {
            return offset / 180f * (float) Math.PI;
        }

        int fps = Math.max(1, GmfConfig.CLIENT.reducedAnimationFps.get());
        float renderSeconds = AnimationTickHolder.getRenderTime(blockEntity.getLevel()) / 20f;
        float sampledSeconds = (float) Math.floor(renderSeconds * fps) / fps;
        float degrees = sampledSeconds * blockEntity.getSpeed() * 6f + offset;
        return (degrees % 360f) / 180f * (float) Math.PI;
    }
}
