package dev.creategmf.gui;

import java.util.Locale;

import dev.creategmf.benchmark.BenchmarkResult;
import dev.creategmf.benchmark.PcBenchmarkSession;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfBenchmarkScreen extends GmfScreen {
    private Button startButton;
    private Button cancelButton;

    public GmfBenchmarkScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.benchmark_pc"), parent);
    }

    @Override
    protected void init() {
        int x = width / 2 - 100;
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.start"), button ->
                        PcBenchmarkSession.INSTANCE.start())
                .bounds(x, 72, 200, 20).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.cancel"), button ->
                        PcBenchmarkSession.INSTANCE.cancel())
                .bounds(x, 96, 200, 20).build());
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
        var state = PcBenchmarkSession.INSTANCE.state();
        boolean running = state == PcBenchmarkSession.State.WARMUP || state == PcBenchmarkSession.State.MEASURING;
        startButton.active = !running;
        cancelButton.active = running;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        var session = PcBenchmarkSession.INSTANCE;
        graphics.drawCenteredString(font,
                Component.translatable("gui.create_gmf.benchmark.state." + session.state().name().toLowerCase(Locale.ROOT)),
                width / 2, 48, 0xFFFFFFFF);
        drawProgress(graphics, session.progress(), 124);
        BenchmarkResult result = session.result();
        if (result != null) {
            int x = width / 2 - 118;
            graphics.drawString(font, Component.translatable("gui.create_gmf.benchmark.cpu_score",
                    number(result.cpuOperationsPerMillisecond())), x, 145, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.metric.fps",
                    number(result.frames().averageFps())), x, 158, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.benchmark.profile",
                    Component.translatable(result.profile().translationKey()),
                    Component.translatable(result.profileEvidence().translationKey())), x, 171, 0xFFE4BB67);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component number(double value) {
        return Component.literal(String.format(Locale.ROOT, "%.1f", value));
    }
}
