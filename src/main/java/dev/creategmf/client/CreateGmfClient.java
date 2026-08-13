package dev.creategmf.client;

import java.util.function.Supplier;

import dev.creategmf.CreateGmf;
import dev.creategmf.gui.GmfSettingsScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CreateGmf.MOD_ID, dist = Dist.CLIENT)
public final class CreateGmfClient {
    public CreateGmfClient(IEventBus modEventBus, ModContainer container) {
        RendererRestartTracker.captureLaunchState();
        modEventBus.addListener(ClientKeyMappings::register);
        modEventBus.addListener(PerformanceOverlay::register);
        NeoForge.EVENT_BUS.addListener(ClientRuntime::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientRuntime::onRenderFrame);

        Supplier<IConfigScreenFactory> screenFactory = () ->
                (minecraft, parent) -> new GmfSettingsScreen(parent);
        container.registerExtensionPoint(IConfigScreenFactory.class, screenFactory);
    }
}
