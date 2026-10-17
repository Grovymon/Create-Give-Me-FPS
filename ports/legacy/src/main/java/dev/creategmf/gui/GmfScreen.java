package dev.creategmf.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.creategmf.legacy.GmfButtons;
import dev.creategmf.legacy.GmfGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class GmfScreen extends Screen {
    protected static final int PANEL_COLOR = 0xE0181D24;
    protected static final int ACCENT_COLOR = 0xFFE4BB67;
    protected final Screen parent;
    protected GmfScreen(Component title, Screen parent) { super(title); this.parent = parent; }
    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        render(new GmfGraphics(pose), mouseX, mouseY, partialTick);
    }
    public void render(GmfGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics.pose(), mouseX, mouseY, partialTick);
    }
    @Override public void renderBackground(PoseStack pose) {}
    protected void addBackButton() {
        addRenderableWidget(GmfButtons.builder(Component.translatable("gui.create_gmf.back"), button -> onClose())
                .bounds(width / 2 - 100, height - 28, 200, 20).build());
    }
    protected void renderPanel(GmfGraphics graphics) {
        graphics.fill(0, 0, width, height, minecraft != null && minecraft.level != null ? 0x68000000 : 0xFF0E1116);
        int panelWidth = Math.min(420, width - 24);
        graphics.fill(width / 2 - panelWidth / 2, 12, width / 2 + panelWidth / 2, height - 8, PANEL_COLOR);
        graphics.drawCenteredString(font, title, width / 2, 22, ACCENT_COLOR);
    }
    protected void drawProgress(GmfGraphics graphics, double progress, int y) {
        int left = width / 2 - 100;
        graphics.fill(left, y, left + 200, y + 8, 0xFF303843);
        graphics.fill(left + 1, y + 1, left + 1 + (int)(198 * Math.max(0, Math.min(1, progress))), y + 7, 0xFF6AAE65);
    }
    @Override public void onClose() { if (minecraft != null) minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
