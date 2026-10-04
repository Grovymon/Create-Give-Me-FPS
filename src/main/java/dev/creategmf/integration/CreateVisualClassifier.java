package dev.creategmf.integration;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.creategmf.config.MechanismAnimationGroup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Structural, optional-addon compatibility: registry/type identity first, Create inheritance second.
 * No addon class is linked. Custom visuals not using Create's base classes fail open.
 * Descriptors are resolved once per type, not per rendered instance or frame.
 */
public final class CreateVisualClassifier {
    private static final ConcurrentHashMap<BlockEntityType<?>, Owner> OWNERS = new ConcurrentHashMap<>();
    private CreateVisualClassifier() {}
    public static Owner classify(BlockEntity entity) {
        return OWNERS.computeIfAbsent(entity.getType(), type -> {
            ResourceLocation id = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(type);
            boolean supported = supported(id == null ? "" : id.getNamespace(),
                    entity instanceof KineticBlockEntity, entity instanceof SmartBlockEntity);
            return new Owner(id == null ? "" : id.toString(), id == null ? "" : id.getNamespace(),
                    supported, MechanismAnimationGroup.fromClassName(entity.getClass().getName()));
        });
    }
    public static boolean supported(String namespace, boolean kinetic, boolean smart) {
        return !"minecraft".equals(namespace) && !namespace.isEmpty()
                && ("create".equals(namespace) || kinetic || smart);
    }
    /** Registry-sized cache; GUI lists only observed compatible addon types, never guesses classes. */
    public static Collection<Owner> observedOwners() { return OWNERS.values(); }
    public record Owner(String registryId, String namespace, boolean supported, MechanismAnimationGroup group) {}
}
