package dev.creategmf.integration;

import java.lang.reflect.Method;

import dev.creategmf.CreateGmf;

import net.neoforged.fml.ModList;

public final class ShaderStatusDetector {
    private ShaderStatusDetector() {
    }

    public static boolean isShaderPackActive() {
        if (!ModList.get().isLoaded("iris")) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method getInstance = apiClass.getMethod("getInstance");
            Object api = getInstance.invoke(null);
            Method isShaderPackInUse = apiClass.getMethod("isShaderPackInUse");
            return Boolean.TRUE.equals(isShaderPackInUse.invoke(api));
        } catch (ReflectiveOperationException | LinkageError error) {
            CreateGmf.LOGGER.debug("[GMF] Iris is installed, but its active shader state is unavailable", error);
            return false;
        }
    }
}
