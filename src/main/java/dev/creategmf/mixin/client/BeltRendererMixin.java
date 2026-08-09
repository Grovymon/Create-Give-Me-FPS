package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltRenderer;
import com.simibubi.create.foundation.render.ShadowRenderHelper;

import dev.creategmf.optimization.belts.BeltShadowOptimizer;

import net.minecraft.client.renderer.MultiBufferSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeltRenderer.class)
public abstract class BeltRendererMixin {
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
