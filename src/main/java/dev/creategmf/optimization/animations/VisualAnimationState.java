package dev.creategmf.optimization.animations;

import dev.creategmf.integration.CreateVisualClassifier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/** Allocated once per visual; never applies world-space policy to contraption-local coordinates. */
public final class VisualAnimationState {
    public final BlockEntity owner;
    public final CreateVisualClassifier.Owner descriptor;
    private AABB sourceBounds;
    private AABB paddedBounds;
    public final FrustumGrace grace = new FrustumGrace();
    public long lastBucket = Long.MIN_VALUE;
    public boolean wasPaused;
    public VisualAnimationState(BlockEntity owner) {
        this.owner = owner;
        descriptor = CreateVisualClassifier.classify(owner);
    }
    public AABB worldBounds() {
        if (!(owner instanceof com.simibubi.create.foundation.blockEntity.CachedRenderBBBlockEntity cached)) return null;
        AABB current = cached.getRenderBoundingBox();
        if (current != sourceBounds) { sourceBounds = current; paddedBounds = current.inflate(2); }
        return paddedBounds;
    }
    public boolean knownWorldOwner() {
        return descriptor.supported() && !owner.isRemoved()
                && owner.getLevel() == Minecraft.getInstance().level;
    }
}
