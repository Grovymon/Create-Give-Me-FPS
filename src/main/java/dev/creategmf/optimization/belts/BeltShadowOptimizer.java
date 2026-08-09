package dev.creategmf.optimization.belts;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;

import dev.creategmf.config.GmfConfig;

import net.minecraft.client.Minecraft;

public final class BeltShadowOptimizer {
    private static long attempted;
    private static long rendered;
    private static long skipped;
    private static volatile Boolean forcedEnabled;

    private BeltShadowOptimizer() {
    }

    public static boolean shouldRender(BeltBlockEntity belt) {
        attempted++;
        Boolean forced = forcedEnabled;
        boolean enabled = forced != null ? forced : GmfConfig.CLIENT.enabled.get()
                && GmfConfig.CLIENT.beltItemShadowOptimization.get();
        if (!enabled) {
            rendered++;
            return true;
        }

        var player = Minecraft.getInstance().player;
        if (player == null) {
            rendered++;
            return true;
        }
        double distance = GmfConfig.CLIENT.beltItemShadowDistance.get();
        boolean shouldRender = player.distanceToSqr(
                belt.getBlockPos().getX() + 0.5,
                belt.getBlockPos().getY() + 0.5,
                belt.getBlockPos().getZ() + 0.5) <= distance * distance;
        if (shouldRender) {
            rendered++;
        } else {
            skipped++;
        }
        return shouldRender;
    }

    public static BeltShadowCounters counters() {
        return new BeltShadowCounters(attempted, rendered, skipped);
    }

    public static void forceEnabled(Boolean enabled) {
        forcedEnabled = enabled;
    }
}
