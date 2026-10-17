package dev.creategmf.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public final class CreateGmfClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientKeyMappings.register();
        PerformanceOverlay.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientRuntime.onClientTick());
    }
}
