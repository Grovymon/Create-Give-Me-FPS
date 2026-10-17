package dev.creategmf.optimization.animations;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.texture.Tickable;
import net.minecraft.resources.ResourceLocation;

public final class SharedFluidAnimationRegistry {
    private static final Set<Tickable> TICKERS = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private SharedFluidAnimationRegistry() {}
    public static void register(ResourceLocation id, Tickable ticker) {
        String path = id.getPath();
        if (ticker != null && (path.endsWith("water_still") || path.endsWith("water_flow")
                || path.endsWith("lava_still") || path.endsWith("lava_flow"))) TICKERS.add(ticker);
    }
    public static boolean shouldFreeze(Tickable ticker) {
        return DistantAnimationController.shouldFreezeSharedFluidTextures() && TICKERS.contains(ticker);
    }
}
