package dev.creategmf.mixin.client;

import dev.creategmf.optimization.occlusion.CreateOcclusionController;
import dev.engine_room.flywheel.lib.visualization.VisualizationHelper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets Entity Culling test block entities rendered exclusively by Flywheel.
 *
 * <p>This is an optional target: {@code GmfMixinPlugin} only enables it when
 * Entity Culling is present and Create: Nowheel is absent. It is adapted from
 * Create: Nowheel under the MIT License; see THIRD_PARTY_NOTICES.md.</p>
 */
@Mixin(targets = "dev.tr7zw.entityculling.CullTask", remap = false)
public abstract class EntityCullingCullTaskMixin {
    @Unique
    private static final BlockEntityRenderer<BlockEntity> CREATE_GMF$NO_OP_RENDERER =
            EntityCullingCullTaskMixin::createGmf$renderNothing;

    @Redirect(
            method = "cullBlockEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderDispatcher;getRenderer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;"
            ),
            require = 0
    )
    private BlockEntityRenderer<BlockEntity> createGmf$includeFlywheelOnlyVisuals(
            BlockEntityRenderDispatcher dispatcher, BlockEntity blockEntity) {
        BlockEntityRenderer<BlockEntity> renderer = dispatcher.getRenderer(blockEntity);
        if (renderer != null || !VisualizationHelper.canVisualize(blockEntity)
                || !CreateOcclusionController.shouldExposeToEntityCulling(blockEntity)) {
            return renderer;
        }
        return CREATE_GMF$NO_OP_RENDERER;
    }

    private static void createGmf$renderNothing(BlockEntity blockEntity, float partialTick, PoseStack poseStack,
            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // Entity Culling needs a renderer marker for Flywheel-only visuals.
        // The visual itself remains owned by Flywheel and is never rendered here.
    }
}
