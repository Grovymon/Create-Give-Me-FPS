package dev.creategmf.integration;

import java.util.ArrayList;
import java.util.List;

import net.neoforged.fml.ModList;

/** Detects optimisation mods that can patch the same Create renderer paths. */
public final class ModCompatibilityDetector {
    private ModCompatibilityDetector() {
    }

    public static List<String> overlappingOptimizers() {
        ModList mods = ModList.get();
        List<String> found = new ArrayList<>(2);
        if (mods.isLoaded("createbetterfps")) found.add("CreateBetterFPS");
        if (mods.isLoaded("flerovium")) found.add("Flerovium");
        return found;
    }
}
