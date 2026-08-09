package dev.creategmf.mixin.client;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;

import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.animations.RotationAnimationRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RotatingInstance.class, remap = false)
public abstract class RotatingInstanceMixin {
    @Unique
    private BlockPos createGmf$worldPosition;
    @Unique
    private String createGmf$ownerClassName;

    @Inject(
            method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/Direction$Axis;F)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
            at = @At("HEAD"),
            require = 0
    )
    private void createGmf$rememberWorldPosition(KineticBlockEntity blockEntity, Axis axis, float speed,
            CallbackInfoReturnable<RotatingInstance> cir) {
        createGmf$worldPosition = blockEntity.getBlockPos();
        createGmf$ownerClassName = blockEntity.getClass().getName();
    }

    @Inject(
            method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/Direction$Axis;F)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
            at = @At("RETURN"),
            require = 0
    )
    private void createGmf$registerRotation(KineticBlockEntity blockEntity, Axis axis, float speed,
            CallbackInfoReturnable<RotatingInstance> cir) {
        RotationAnimationRegistry.register(cir.getReturnValue(), createGmf$worldPosition, createGmf$ownerClassName);
    }
}
