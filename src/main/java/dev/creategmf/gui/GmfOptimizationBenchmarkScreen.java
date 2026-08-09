package dev.creategmf.gui;

import java.util.Locale;

import dev.creategmf.benchmark.OptimizationBenchmarkResult;
import dev.creategmf.benchmark.OptimizationBenchmarkSession;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfOptimizationBenchmarkScreen extends GmfScreen {
    private Button startButton;
    private Button cancelButton;

    public GmfOptimizationBenchmarkScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.benchmark_optimization"), parent);
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.start_ab"), button -> {
                        OptimizationBenchmarkSession.INSTANCE.start();
                        minecraft.setScreen(null);
                    })
                .bounds(x, 62, 220, 20).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.cancel"), button ->
                        OptimizationBenchmarkSession.INSTANCE.cancel())
                .bounds(x, 86, 220, 20).build());
        addBackButton();
        updateButtons();
    }

    @Override
    public void tick() {
        updateButtons();
    }

    private void updateButtons() {
        if (startButton == null) {
            return;
        }
        var state = OptimizationBenchmarkSession.INSTANCE.state();
        boolean running = OptimizationBenchmarkSession.INSTANCE.isActive();
        startButton.active = !running;
        cancelButton.active = running;
    }

    @Override
    public void onClose() {
        var state = OptimizationBenchmarkSession.INSTANCE.state();
        if (OptimizationBenchmarkSession.INSTANCE.isActive()) {
            OptimizationBenchmarkSession.INSTANCE.cancel();
        }
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        var session = OptimizationBenchmarkSession.INSTANCE;
        graphics.drawCenteredString(font,
                Component.translatable("gui.create_gmf.ab.state." + session.state().name().toLowerCase(Locale.ROOT)),
                width / 2, 43, 0xFFFFFFFF);
        drawProgress(graphics, session.progress(), 114);
        OptimizationBenchmarkResult result = session.result();
        if (result != null) {
            int x = width / 2 - 125;
            graphics.drawString(font, Component.translatable("gui.create_gmf.ab.fps",
                    number(result.beforeFrames().averageFps()), number(result.afterFrames().averageFps())),
                    x, 136, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.ab.frame_time",
                    number(result.beforeFrames().averageMilliseconds()),
                    number(result.afterFrames().averageMilliseconds())), x, 151, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.ab.one_low",
                    number(result.beforeFrames().onePercentLow()), number(result.afterFrames().onePercentLow())),
                    x, 166, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.ab.skipped",
                    result.afterCounters().skipped()), x, 181, 0xFFE4BB67);
            if (result.afterCounters().attempted() == 0) {
                graphics.drawWordWrap(font, Component.translatable("gui.create_gmf.ab.no_belt_shadows"),
                        x, 196, 250, 0xFFFFA06A);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component number(double value) {
        return Component.literal(String.format(Locale.ROOT, "%.1f", value));
    }
}
