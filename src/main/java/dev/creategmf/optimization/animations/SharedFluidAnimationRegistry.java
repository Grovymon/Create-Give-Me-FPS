package dev.creategmf.optimization.animations;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteTicker;

public final class SharedFluidAnimationRegistry {
    private static final Set<SpriteTicker> FLUID_TICKERS =
            Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    private SharedFluidAnimationRegistry() {
    }

    public static void register(SpriteContents contents, SpriteTicker ticker) {
        if (isWaterOrLava(contents.name().getPath())) {
            FLUID_TICKERS.add(ticker);
        }
    }

    public static boolean shouldFreeze(SpriteTicker ticker) {
        return DistantAnimationController.shouldFreezeSharedFluidTextures()
                && FLUID_TICKERS.contains(ticker);
    }

    private static boolean isWaterOrLava(String path) {
        return path.endsWith("water_still") || path.endsWith("water_flow")
                || path.endsWith("lava_still") || path.endsWith("lava_flow");
    }
}
