package dev.creategmf.mixin.client;

import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import com.jozufozu.flywheel.backend.instancing.AbstractInstance;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.creategmf.optimization.animations.RotationAnimationRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = InstanceManager.class, remap = false)
public abstract class LegacyInstanceConstructionMixin {
    // Flywheel invokes init() AFTER the registry factory returns. Keep the
    // owner bound across both constructor and init, releasing it on exceptions.
    @WrapMethod(method = "createInternal", require = 1)
    private AbstractInstance createGmf$owner(Object owner, Operation<AbstractInstance> original) {
        if (!(owner instanceof BlockEntity entity)) return original.call(owner);
        RotationAnimationRegistry.beginConstruction(entity);
        try { return original.call(owner); }
        finally { RotationAnimationRegistry.endConstruction(); }
    }
}
