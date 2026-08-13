package dev.creategmf.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.animations.DistantAnimationController.AnimationPolicy;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction.Axis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the large cog and its exposed shaft on the same sampled angle. */
@Mixin(value = BracketedKineticBlockEntityRenderer.class, remap = false)
public abstract class LargeCogRendererMixin {
    @ModifyReturnValue(method = "getAngleForLargeCogShaft", at = @At("RETURN"), require = 1)
    private static float createGmf$sampleLargeCogAngle(float original, SimpleKineticBlockEntity cog, Axis axis) {
        AnimationPolicy policy = DistantAnimationController.animationPolicy(cog.getBlockPos(),
                MechanismAnimationGroup.SHAFTS_AND_COGS,
                Minecraft.getInstance().gameRenderer.getMainCamera());
        if (policy == AnimationPolicy.FULL) {
            return original;
        }
        float offset = BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, cog.getBlockPos());
        if (policy == AnimationPolicy.STATIC) {
            return offset / 180f * (float) Math.PI;
        }
        int fps = Math.max(1, GmfConfig.CLIENT.reducedAnimationFps.get());
        float seconds = AnimationTickHolder.getRenderTime(cog.getLevel()) / 20f;
        float sampledSeconds = (float) Math.floor(seconds * fps) / fps;
        float degrees = sampledSeconds * cog.getSpeed() * 6f + offset;
        return (degrees % 360f) / 180f * (float) Math.PI;
    }
}
