package dev.creategmf.legacy;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class LegacyVisuals {
    private LegacyVisuals() {}
    public static void queueUpdate(BlockEntity entity) { InstancedRenderDispatcher.enqueueUpdate(entity); }
    public static boolean canVisualize(BlockEntity entity) {
        return entity != null && entity.getLevel() != null && Backend.isOn()
                && InstancedRenderRegistry.canInstance(entity.getType());
    }
    public static LegacyVisualManager manager(Level level) {
        if (level == null || !Backend.isOn()) return null;
        var manager = InstancedRenderDispatcher.getBlockEntities(level);
        return manager == null ? null : new LegacyVisualManager(manager);
    }
}
