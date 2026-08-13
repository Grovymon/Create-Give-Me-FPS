package dev.creategmf.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBehaviour;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelConnection;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelRenderer;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Factory-panel paths and bulbs are decorative animated CPU geometry, not game logic. */
@Mixin(value = FactoryPanelRenderer.class, remap = false)
public abstract class FactoryPanelRendererMixin {
    @Inject(method = "renderPath", at = @At("HEAD"), cancellable = true, require = 1)
    private static void createGmf$skipPanelPath(FactoryPanelBehaviour panel, FactoryPanelConnection connection,
            float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay,
            CallbackInfo ci) {
        if (isStatic(panel)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderBulb", at = @At("HEAD"), cancellable = true, require = 1)
    private static void createGmf$skipPanelBulb(FactoryPanelBehaviour panel, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, CallbackInfo ci) {
        if (isStatic(panel)) {
            ci.cancel();
        }
    }

    private static boolean isStatic(FactoryPanelBehaviour panel) {
        return DistantAnimationController.animationPolicy(panel.panelBE().getBlockPos(),
                MechanismAnimationGroup.TUNNELS_AND_FUNNELS,
                net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera())
                == DistantAnimationController.AnimationPolicy.STATIC;
    }
}
