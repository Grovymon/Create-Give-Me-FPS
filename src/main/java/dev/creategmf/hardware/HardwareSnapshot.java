package dev.creategmf.hardware;

public record HardwareSnapshot(
        String cpuName,
        int logicalProcessors,
        int physicalCores,
        String gpuName,
        String gpuVendor,
        long vramBytes,
        long systemRamBytes,
        long heapMaxBytes,
        int windowWidth,
        int windowHeight,
        boolean fullscreen,
        int renderDistance,
        int simulationDistance,
        boolean vsync,
        int fpsLimit,
        String flywheelBackend,
        String shaderLoader,
        String activeShaderPack,
        String rendererMod,
        boolean distantHorizonsPresent
) {
}
