package dev.creategmf.legacy;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstanceManager;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Main-thread hand-off to Flywheel 0.6; no kinetic network mutations. */
public final class LegacyVisualManager {
    private final InstanceManager<BlockEntity> manager;
    public LegacyVisualManager(InstanceManager<BlockEntity> manager) { this.manager = manager; }
    public void queueAdd(BlockEntity entity) { manager.queueAdd(entity); }
    public void queueRemove(BlockEntity entity) {
        // 0.6 has no queueRemove. Wait for pending visual workers before using
        // its public removal API, avoiding concurrent instance-map mutation.
        var tasks = Backend.getTaskEngine();
        if (tasks != null) tasks.syncPoint();
        manager.remove(entity);
    }
}
