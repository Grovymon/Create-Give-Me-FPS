package dev.creategmf.hardware;

import java.lang.management.ManagementFactory;

import org.lwjgl.opengl.GL11;

import dev.engine_room.flywheel.api.backend.Backend;
import dev.engine_room.flywheel.api.backend.BackendManager;

import net.minecraft.client.Minecraft;
import net.neoforged.fml.ModList;

public final class HardwareDetector {
    private HardwareDetector() {
    }

    public static HardwareSnapshot capture() {
        Minecraft minecraft = Minecraft.getInstance();
        Runtime runtime = Runtime.getRuntime();
        ModList mods = ModList.get();

        String cpu = System.getenv("PROCESSOR_IDENTIFIER");
        if (cpu == null || cpu.isBlank()) {
            cpu = System.getProperty("os.arch", "unknown");
        }

        long systemRam = -1;
        int physicalCores = -1;
        var operatingSystem = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystem instanceof com.sun.management.OperatingSystemMXBean extended) {
            systemRam = extended.getTotalMemorySize();
        }

        String gpu = safeGlString(GL11.GL_RENDERER);
        String gpuVendor = safeGlString(GL11.GL_VENDOR);
        String flywheel = "unavailable";
        try {
            flywheel = Backend.REGISTRY.getIdOrThrow(BackendManager.currentBackend()).toString();
        } catch (RuntimeException ignored) {
            // The backend may not be selected until a level is rendering.
        }

        String shaderLoader = mods.isLoaded("iris") ? "Iris"
                : mods.isLoaded("oculus") ? "Oculus"
                : "none detected";
        String renderer = mods.isLoaded("sodium") ? "Sodium"
                : mods.isLoaded("embeddium") ? "Embeddium"
                : "vanilla/Flywheel";

        var window = minecraft.getWindow();
        return new HardwareSnapshot(
                cpu,
                runtime.availableProcessors(),
                physicalCores,
                gpu,
                gpuVendor,
                -1,
                systemRam,
                runtime.maxMemory(),
                window.getWidth(),
                window.getHeight(),
                window.isFullscreen(),
                minecraft.options.renderDistance().get(),
                minecraft.options.simulationDistance().get(),
                minecraft.options.enableVsync().get(),
                minecraft.options.framerateLimit().get(),
                flywheel,
                shaderLoader,
                "unavailable",
                renderer,
                mods.isLoaded("distanthorizons") || mods.isLoaded("distant_horizons")
        );
    }

    private static String safeGlString(int name) {
        try {
            String value = GL11.glGetString(name);
            return value == null ? "unavailable" : value;
        } catch (RuntimeException ignored) {
            return "unavailable";
        }
    }
}
