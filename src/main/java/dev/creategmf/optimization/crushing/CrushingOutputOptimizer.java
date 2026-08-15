package dev.creategmf.optimization.crushing;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlockEntity;

import dev.creategmf.config.GmfConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Hides only the client rendering of loose ItemEntity instances immediately at
 * crushing wheels. It never removes an entity or changes its server-side tick.
 */
public final class CrushingOutputOptimizer {
    private static final double HIDE_RADIUS_SQUARED = 4.5 * 4.5;
    private static final long RESCAN_INTERVAL_TICKS = 20;
    private static ClientLevel cachedLevel;
    private static long nextRescanTick;
    private static List<BlockPos> crusherControllers = List.of();

    private CrushingOutputOptimizer() {
    }

    public static boolean shouldRender(ItemEntity item) {
        if (GmfConfig.CLIENT.crushingOutputRendering.get()) {
            return true;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return true;
        }
        refresh(level);
        for (BlockPos position : crusherControllers) {
            if (position.distToCenterSqr(item.position()) <= HIDE_RADIUS_SQUARED) {
                return false;
            }
        }
        return true;
    }

    private static void refresh(ClientLevel level) {
        long gameTime = level.getGameTime();
        if (cachedLevel == level && gameTime < nextRescanTick) {
            return;
        }
        cachedLevel = level;
        nextRescanTick = gameTime + RESCAN_INTERVAL_TICKS;
        var player = Minecraft.getInstance().player;
        if (player == null) {
            crusherControllers = List.of();
            return;
        }
        int centerX = player.chunkPosition().x;
        int centerZ = player.chunkPosition().z;
        List<BlockPos> found = new ArrayList<>();
        for (int x = centerX - 3; x <= centerX + 3; x++) {
            for (int z = centerZ - 3; z <= centerZ + 3; z++) {
                if (!level.hasChunk(x, z)) {
                    continue;
                }
                for (BlockEntity blockEntity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (blockEntity instanceof CrushingWheelControllerBlockEntity) {
                        found.add(blockEntity.getBlockPos().immutable());
                    }
                }
            }
        }
        crusherControllers = List.copyOf(found);
    }
}
