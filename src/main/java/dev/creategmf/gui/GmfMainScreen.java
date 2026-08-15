package dev.creategmf.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfMainScreen extends GmfScreen {
    public GmfMainScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.title"), parent);
    }

    @Override
    protected void init() {
        // The main GMF key now opens its actual settings directly. Diagnostics
        // remain a separate workflow and the belt benchmark is no longer exposed
        // as a second entry screen.
        minecraft.setScreen(new GmfSettingsScreen(parent));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
