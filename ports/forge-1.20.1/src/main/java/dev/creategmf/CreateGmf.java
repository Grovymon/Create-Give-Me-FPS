package dev.creategmf;

import com.mojang.logging.LogUtils;
import dev.creategmf.client.CreateGmfClient;
import dev.creategmf.config.GmfConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CreateGmf.MOD_ID)
public final class CreateGmf {
    public static final String MOD_ID = "create_gmf";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateGmf() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, GmfConfig.SPEC);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> CreateGmfClient::register);
        LOGGER.info("[GMF] Create: Give Me FPS 0.1.3.3 initializing on Minecraft 1.20.1");
    }
}
