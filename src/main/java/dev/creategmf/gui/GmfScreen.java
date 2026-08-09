package dev.creategmf.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class GmfScreen extends Screen {
    protected static final int PANEL_COLOR = 0xE0181D24;
    protected static final int ACCENT_COLOR = 0xFFE4BB67;
    protected final Screen parent;

    protected GmfScreen(Component title, Screen parent) {
        super(title);
        this.parent = parent;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Gmf screens draw their own background in renderPanel(). The vanilla
        // implementation runs a blur pass after our labels but before widgets,
        // which made only the text appear blurred.
    }

    protected void addBackButton() {
        addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.back"), button -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20)
                .build());
    }

    protected void renderPanel(GuiGraphics graphics) {
        if (minecraft != null && minecraft.level != null) {
            // Keep the running world visible. Unlike renderBackground(), this does
            // not invoke Minecraft's blur post-process.
            graphics.fill(0, 0, width, height, 0x68000000);
        } else {
            graphics.fill(0, 0, width, height, 0xFF0E1116);
        }
        int panelWidth = Math.min(420, width - 24);
        graphics.fill(width / 2 - panelWidth / 2, 12, width / 2 + panelWidth / 2, height - 8, PANEL_COLOR);
        graphics.drawCenteredString(font, title, width / 2, 22, ACCENT_COLOR);
    }

    protected void drawProgress(GuiGraphics graphics, double progress, int y) {
        int left = width / 2 - 100;
        graphics.fill(left, y, left + 200, y + 8, 0xFF303843);
        graphics.fill(left + 1, y + 1, left + 1 + (int) (198 * Math.max(0, Math.min(1, progress))), y + 7,
                0xFF6AAE65);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
