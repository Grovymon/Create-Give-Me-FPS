package dev.creategmf.diagnostics;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import dev.creategmf.config.MechanismAnimationGroup;

public record SceneCensus(
        int chunksScanned,
        int createBlockEntities,
        int kineticBlockEntities,
        int beltControllers,
        int transportedItems,
        int contraptions,
        int looseItemEntities,
        Map<MechanismAnimationGroup, Integer> mechanismCounts,
        Map<String, Integer> blockEntityTypeCounts
) {
    public static final SceneCensus EMPTY = new SceneCensus(0, 0, 0, 0, 0, 0, 0, Map.of(), Map.of());

    public List<MechanismLoad> topMechanisms(int limit) {
        return mechanismCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new MechanismLoad(entry.getKey(), entry.getValue(),
                        entry.getValue() * visualWeight(entry.getKey())))
                .sorted(Comparator.comparingInt(MechanismLoad::estimatedWeight).reversed())
                .limit(limit)
                .toList();
    }

    public List<BlockEntityLoad> topBlockEntityTypes(int limit) {
        return blockEntityTypeCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> new BlockEntityLoad(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(BlockEntityLoad::objects).reversed())
                .limit(limit)
                .toList();
    }

    private static int visualWeight(MechanismAnimationGroup group) {
        return switch (group) {
            case CRUSHERS_AND_MILLSTONES, CHAINS_AND_CONVEYORS, BELTS, TRAINS_AND_RAILS -> 4;
            case BEARINGS_AND_CONTRAPTIONS, STEAM_ENGINES, MECHANICAL_ARMS, BLAZE_BURNERS -> 3;
            case TUNNELS_AND_FUNNELS, EJECTORS, PACKAGE_PORTS, PUMPS_AND_PIPES -> 2;
            default -> 1;
        };
    }
}
