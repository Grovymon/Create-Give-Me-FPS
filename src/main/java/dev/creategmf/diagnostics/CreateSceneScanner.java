package dev.creategmf.diagnostics;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CreateSceneScanner {
    private static final int CHUNK_RADIUS = 3;

    private CreateSceneScanner() {
    }

    public static SceneCensus captureNearby() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return SceneCensus.EMPTY;
        }

        int centerX = minecraft.player.chunkPosition().x;
        int centerZ = minecraft.player.chunkPosition().z;
        int chunks = 0;
        int createBlockEntities = 0;
        int kinetics = 0;
        int belts = 0;
        int items = 0;

        for (int x = centerX - CHUNK_RADIUS; x <= centerX + CHUNK_RADIUS; x++) {
            for (int z = centerZ - CHUNK_RADIUS; z <= centerZ + CHUNK_RADIUS; z++) {
                if (!level.hasChunk(x, z)) {
                    continue;
                }
                chunks++;
                for (BlockEntity blockEntity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (blockEntity.getClass().getName().startsWith("com.simibubi.create.")) {
                        createBlockEntities++;
                    }
                    if (blockEntity instanceof KineticBlockEntity) {
                        kinetics++;
                    }
                    if (blockEntity instanceof BeltBlockEntity belt && belt.isController()) {
                        belts++;
                        items += belt.getInventory().getTransportedItems().size();
                        if (belt.getInventory().getLazyClientItem() != null) {
                            items++;
                        }
                    }
                }
            }
        }

        int contraptions = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof AbstractContraptionEntity) {
                contraptions++;
            }
        }
        return new SceneCensus(chunks, createBlockEntities, kinetics, belts, items, contraptions);
    }
}
