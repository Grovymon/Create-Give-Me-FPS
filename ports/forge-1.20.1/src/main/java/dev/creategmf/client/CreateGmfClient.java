package dev.creategmf.client;

import dev.creategmf.gui.GmfSettingsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public final class CreateGmfClient {
    private CreateGmfClient() {}

    public static void register() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(ClientKeyMappings::register);
        modEventBus.addListener(PerformanceOverlay::register);
        MinecraftForge.EVENT_BUS.addListener(ClientRuntime::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(ClientRuntime::onRenderFrame);
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> new GmfSettingsScreen(parent)));
    }
}
