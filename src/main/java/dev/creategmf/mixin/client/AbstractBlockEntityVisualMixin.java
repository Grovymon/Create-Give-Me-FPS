package dev.creategmf.mixin.client;

import dev.creategmf.optimization.animations.BlockEntityVisualAccess;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;

import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import dev.creategmf.optimization.animations.VisualAnimationState;
import net.minecraft.world.level.block.entity.BlockEntity;

@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public abstract class AbstractBlockEntityVisualMixin implements BlockEntityVisualAccess {
    @Shadow
    @Final
    protected BlockPos pos;
    @Shadow @Final protected BlockEntity blockEntity;
    @Unique private volatile VisualAnimationState createGmf$animation;
    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>",
            at = @org.spongepowered.asm.mixin.injection.At("TAIL"), require = 1)
    private void createGmf$registerVisual(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        createGmf$animation = new VisualAnimationState(blockEntity);
        dev.creategmf.optimization.animations.VisualPopulation.add(createGmf$animation);
    }

    @Override
    public VisualAnimationState createGmf$getAnimationState() {
        VisualAnimationState state = createGmf$animation;
        if (state == null) createGmf$animation = state = new VisualAnimationState(blockEntity);
        return state;
    }

    @Override
    public BlockPos createGmf$getWorldPosition() {
        return pos;
    }
}
