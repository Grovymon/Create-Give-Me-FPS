package dev.creategmf;

import com.mojang.logging.LogUtils;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.profiler.ServerTickCollector;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

public final class CreateGmf implements ModInitializer {
    public static final String MOD_ID = "create_gmf";
    public static final Logger LOGGER = LogUtils.getLogger();
    @Override public void onInitialize() {
        ForgeConfigRegistry.INSTANCE.register(MOD_ID, ModConfig.Type.CLIENT, GmfConfig.SPEC);
        ServerTickEvents.START_SERVER_TICK.register(server -> ServerTickCollector.onServerTickPre());
        ServerTickEvents.END_SERVER_TICK.register(server -> ServerTickCollector.onServerTickPost());
        LOGGER.info("[GMF] Create: Give Me FPS 0.1.3.3 initializing on Minecraft 1.20.1 Fabric");
    }
}
