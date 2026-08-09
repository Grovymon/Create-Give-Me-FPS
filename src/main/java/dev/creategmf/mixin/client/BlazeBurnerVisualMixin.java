package dev.creategmf.mixin.client;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerVisual;
import com.simibubi.create.content.processing.burner.ScrollInstance;

import dev.creategmf.optimization.animations.ScrollableVisualAccess;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BlazeBurnerVisual.class, remap = false)
public abstract class BlazeBurnerVisualMixin implements ScrollableVisualAccess {
    @Shadow
    private BlazeBurnerBlock.HeatLevel heatLevel;
    @Shadow
    private ScrollInstance flame;

    @Unique
    private ScrollInstance createGmf$trackedFlame;
    @Unique
    private boolean createGmf$suppressed;

    @Override
    public void createGmf$setAnimationSuppressed(boolean suppressed) {
        if (flame == null) {
            createGmf$trackedFlame = null;
            createGmf$suppressed = suppressed;
            return;
        }
        if (flame == createGmf$trackedFlame && suppressed == createGmf$suppressed) {
            return;
        }
        float speed = 1 / 32f + 1 / 64f * heatLevel.ordinal();
        flame.speed(suppressed ? 0 : speed / 2, suppressed ? 0 : speed).setChanged();
        createGmf$trackedFlame = flame;
        createGmf$suppressed = suppressed;
    }
}
