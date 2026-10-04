package dev.creategmf.gui;

import java.util.Locale;

import dev.creategmf.benchmark.OptimizationBenchmarkResult;
import dev.creategmf.benchmark.OptimizationBenchmarkSession;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfOptimizationBenchmarkScreen extends GmfScreen {
    private int scroll;
    private int contentHeight;
    private Button startButton;
    private Button cancelButton;
    private dev.creategmf.benchmark.OptimizationTarget target = dev.creategmf.benchmark.OptimizationTarget.BELT_SHADOWS;

    public GmfOptimizationBenchmarkScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.benchmark_optimization"), parent);
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        addRenderableWidget(Button.builder(Component.translatable(target.translationKey()), button -> {
            var targets = dev.creategmf.benchmark.OptimizationTarget.values();
            target = targets[(target.ordinal() + 1) % targets.length];
            scroll = 0;
            button.setMessage(Component.translatable(target.translationKey()));
            updateButtons();
        }).bounds(x, 40, 220, 20).build());
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.start_ab"), button -> {
                        if (OptimizationBenchmarkSession.INSTANCE.start(target)) minecraft.setScreen(null);
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
        startButton.active = !running && target.runtimeSafe() && minecraft.level != null;
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
                width / 2, 109, 0xFFFFFFFF);
        drawProgress(graphics, session.progress(), 114);
        OptimizationBenchmarkResult result = session.result();
        int w = Math.max(1, Math.min(360, width - 40)), x = width / 2 - w / 2;
        java.util.List<Component> lines = new java.util.ArrayList<>();
        if (!target.runtimeSafe()) lines.add(Component.translatable("gui.create_gmf.ab.restart_experiment"));
        if (result != null) {
            lines.add(Component.translatable(result.target().translationKey()));
            lines.add(Component.translatable("gui.create_gmf.ab.fps", number(result.beforeFrames().averageFps()), number(result.afterFrames().averageFps())));
            lines.add(Component.translatable("gui.create_gmf.ab.frame_time", number(result.beforeFrames().averageMilliseconds()), number(result.afterFrames().averageMilliseconds())));
            lines.add(Component.translatable("gui.create_gmf.ab.one_low", number(result.beforeFrames().onePercentLow()), number(result.afterFrames().onePercentLow())));
            lines.add(Component.translatable("gui.create_gmf.ab.percentiles", number(result.beforeFrames().p95Milliseconds()), number(result.afterFrames().p95Milliseconds()), number(result.beforeFrames().p99Milliseconds()), number(result.afterFrames().p99Milliseconds())));
            lines.add(Component.translatable("gui.create_gmf.ab.gc", result.beforeGcCount(), result.afterGcCount(), result.beforeGcMs(), result.afterGcMs()));
            lines.add(Component.translatable("gui.create_gmf.ab.mspt_max", number(result.beforeFrames().maximumMilliseconds()), number(result.afterFrames().maximumMilliseconds()), available(result.beforeMspt()), available(result.afterMspt())));
            var before = result.beforeEvidence();
            var after = result.afterEvidence();
            lines.add(Component.translatable("gui.create_gmf.ab.evidence", before.animation().skippedUpdates(), after.animation().skippedUpdates(),
                    before.transportedSkipped(), after.transportedSkipped(), before.particlesSuppressed(), after.particlesSuppressed()));
            lines.add(Component.translatable("gui.create_gmf.ab.caution"));
        }
        int y = 132 - scroll;
        graphics.enableScissor(x, 130, x + w, Math.max(130, height - 34));
        for (Component line : lines) {
            graphics.drawWordWrap(font, line, x, y, w, 0xFFD0D8E0);
            y += font.split(line, w).size() * font.lineHeight + 4;
        }
        graphics.disableScissor();
        contentHeight = y + scroll - 132;
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.clamp(scroll - (int) (dy * 18), 0, Math.max(0, contentHeight - Math.max(0, height - 166)));
        return true;
    }

    private static Component number(double value) {
        return value <= 0 ? Component.translatable("gui.create_gmf.unavailable") : Component.literal(String.format(Locale.ROOT, "%.1f", value));
    }
    private static Component available(double value) { return number(value); }
}
