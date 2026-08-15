package dev.creategmf.diagnostics;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;

import dev.creategmf.config.MechanismAnimationGroup;

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
        EnumMap<MechanismAnimationGroup, Integer> mechanismCounts = new EnumMap<>(MechanismAnimationGroup.class);
        Map<String, Integer> blockEntityTypeCounts = new HashMap<>();

        for (int x = centerX - CHUNK_RADIUS; x <= centerX + CHUNK_RADIUS; x++) {
            for (int z = centerZ - CHUNK_RADIUS; z <= centerZ + CHUNK_RADIUS; z++) {
                if (!level.hasChunk(x, z)) {
                    continue;
                }
                chunks++;
                for (BlockEntity blockEntity : level.getChunk(x, z).getBlockEntities().values()) {
                    if (blockEntity.getClass().getName().startsWith("com.simibubi.create.")) {
                        createBlockEntities++;
                        MechanismAnimationGroup group = MechanismAnimationGroup
                                .fromClassName(blockEntity.getClass().getName());
                        mechanismCounts.merge(group, 1, Integer::sum);
                        blockEntityTypeCounts.merge(displayName(blockEntity.getClass().getSimpleName()), 1, Integer::sum);
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
        int looseItemEntities = 0;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof AbstractContraptionEntity) {
                contraptions++;
            }
            if (entity instanceof net.minecraft.world.entity.item.ItemEntity) {
                looseItemEntities++;
            }
        }
        return new SceneCensus(chunks, createBlockEntities, kinetics, belts, items, contraptions,
                looseItemEntities, Map.copyOf(mechanismCounts), Map.copyOf(blockEntityTypeCounts));
    }

    private static String displayName(String className) {
        String stripped = className.replace("BlockEntity", "");
        return stripped.replaceAll("(?<!^)([A-Z])", " $1").trim();
    }
}
