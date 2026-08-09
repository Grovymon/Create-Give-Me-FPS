package dev.creategmf.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfMainScreen extends GmfScreen {
    public GmfMainScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.title"), parent);
    }

    @Override
    protected void init() {
        int x = width / 2 - 120;
        int y = 105;
        int w = 240;
        addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.find_lag_source"),
                        button -> minecraft.setScreen(new GmfDiagnosticsScreen(this)))
                .bounds(x, y, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.create_settings"),
                        button -> minecraft.setScreen(new GmfSettingsScreen(this)))
                .bounds(x, y + 24, w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.benchmark_optimization"),
                        button -> minecraft.setScreen(new GmfOptimizationBenchmarkScreen(this)))
                .bounds(x, y + 48, w, 20).build());
        addBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
