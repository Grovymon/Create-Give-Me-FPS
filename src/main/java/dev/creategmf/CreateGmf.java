package dev.creategmf;

import com.mojang.logging.LogUtils;
import dev.creategmf.config.GmfConfig;
import org.slf4j.Logger;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(CreateGmf.MOD_ID)
public final class CreateGmf {
    public static final String MOD_ID = "create_gmf";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateGmf(IEventBus modEventBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, GmfConfig.SPEC);
        LOGGER.info("[GMF] Create: Give Me FPS {} initializing", container.getModInfo().getVersion());
    }
}
