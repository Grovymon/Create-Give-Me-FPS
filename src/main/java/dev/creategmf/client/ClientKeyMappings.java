package dev.creategmf.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

public final class ClientKeyMappings {
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.create_gmf.open_menu",
            GLFW.GLFW_KEY_G,
            "key.categories.create_gmf");

    private ClientKeyMappings() {
    }
    public static final KeyMapping TOGGLE_PERFORMANCE = new KeyMapping(
            "key.create_gmf.toggle_performance", GLFW.GLFW_KEY_UNKNOWN, "key.categories.create_gmf");

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        event.register(TOGGLE_PERFORMANCE);
    }
}
