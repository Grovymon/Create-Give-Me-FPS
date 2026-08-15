package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.foundation.render.ShadowRenderHelper;

import dev.creategmf.optimization.belts.BeltShadowOptimizer;
import dev.creategmf.diagnostics.GmfRuntimeStatus;

import net.minecraft.client.renderer.MultiBufferSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeltRenderer.class)
public abstract class BeltRendererMixin {
    @ModifyExpressionValue(method = "renderSafe", at = @At(value = "INVOKE",
            target = "Lnet/createmod/catnip/animation/AnimationTickHolder;getRenderTime(Lnet/minecraft/world/level/LevelAccessor;)F"),
            require = 1)
    private float createGmf$sampleBeltScroll(float original, BeltBlockEntity belt) {
        return dev.creategmf.optimization.animations.DistantAnimationController.sampledRenderTime(original,
                belt.getBlockPos(), dev.creategmf.config.MechanismAnimationGroup.BELTS);
    }

    /** Transported items follow server-side belt positions; hide only that moving overlay when static. */
    @Inject(method = "renderItems", at = @At("HEAD"), cancellable = true, require = 1)
    private void createGmf$skipMovingBeltItems(BeltBlockEntity belt, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, CallbackInfo ci) {
        GmfRuntimeStatus.markBeltItemHook();
        if (!dev.creategmf.config.GmfConfig.CLIENT.renderTransportedBeltItems.get()
                || dev.creategmf.optimization.animations.DistantAnimationController.animationPolicy(belt.getBlockPos(),
                dev.creategmf.config.MechanismAnimationGroup.BELTS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy.STATIC) {
            ci.cancel();
        }
    }
    @WrapWithCondition(
            method = "renderItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/foundation/render/ShadowRenderHelper;renderShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;FF)V"
            ),
            require = 0
    )
    private boolean createGmf$limitDistantItemShadows(PoseStack poseStack, MultiBufferSource buffer,
            float alpha, float radius, BeltBlockEntity belt) {
        return BeltShadowOptimizer.shouldRender(belt);
    }
}
