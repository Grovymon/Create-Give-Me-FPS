package dev.creategmf.client;

import java.util.Locale;

import dev.creategmf.CreateGmf;
import dev.creategmf.benchmark.OptimizationBenchmarkSession;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.diagnostics.DiagnosticSession;
import dev.creategmf.profiler.FrameStatistics;
import dev.creategmf.profiler.FrameTimeCollector;
import dev.creategmf.profiler.ServerTickCollector;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw.Layer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

public final class PerformanceOverlay {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(CreateGmf.MOD_ID, "statistics");
    private static final Layer LAYER = PerformanceOverlay::render;

    private PerformanceOverlay() {
    }

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, ID, LAYER);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui) {
            return;
        }
        if (GmfConfig.CLIENT.enabled.get() && GmfConfig.CLIENT.showOverlay.get()) {
            FrameStatistics stats = FrameTimeCollector.INSTANCE.snapshot();
            double mspt = ServerTickCollector.averageMspt();
            int width = 132;
            int height = 42;
            graphics.fill(5, 5, 5 + width, 5 + height, 0xB010141A);
            graphics.drawString(minecraft.font,
                    Component.translatable("gui.create_gmf.overlay.fps", minecraft.getFps()), 10, 10, 0xFFFFFF);
            graphics.drawString(minecraft.font,
                    Component.translatable("gui.create_gmf.overlay.frame_time", number(stats.averageMilliseconds())),
                    10, 22, 0xD0D8E0);
            Component msptValue = mspt > 0 ? number(mspt) : Component.translatable("gui.create_gmf.unavailable");
            graphics.drawString(minecraft.font,
                    Component.translatable("gui.create_gmf.overlay.mspt", msptValue), 10, 34, 0xD0D8E0);
        }
        if (DiagnosticSession.INSTANCE.isActive()) {
            renderWorldMeasurement(graphics, minecraft,
                    Component.translatable("gui.create_gmf.world_diagnostics.title"),
                    Component.translatable(DiagnosticSession.INSTANCE.state() == DiagnosticSession.State.PREPARING
                            ? "gui.create_gmf.world_diagnostics.preparing"
                            : "gui.create_gmf.world_diagnostics.recording"),
                    DiagnosticSession.INSTANCE.progress());
        } else if (OptimizationBenchmarkSession.INSTANCE.isActive()) {
            renderWorldMeasurement(graphics, minecraft,
                    Component.translatable("gui.create_gmf.world_ab.title"),
                    Component.translatable("gui.create_gmf.ab.state."
                            + OptimizationBenchmarkSession.INSTANCE.state().name().toLowerCase(Locale.ROOT)),
                    OptimizationBenchmarkSession.INSTANCE.progress());
        }
    }

    private static void renderWorldMeasurement(GuiGraphics graphics, Minecraft minecraft, Component title,
            Component phase, double progress) {
        int panelWidth = Math.min(330, graphics.guiWidth() - 20);
        int left = (graphics.guiWidth() - panelWidth) / 2;
        int top = 8;
        graphics.fill(left, top, left + panelWidth, top + 52, 0xD010141A);
        graphics.drawCenteredString(minecraft.font, title, graphics.guiWidth() / 2, top + 5, 0xFFE4BB67);
        graphics.drawCenteredString(minecraft.font, phase, graphics.guiWidth() / 2, top + 17, 0xFFFFFFFF);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("gui.create_gmf.world_measurement.instruction"),
                graphics.guiWidth() / 2, top + 29, 0xFFD0D8E0);
        graphics.drawCenteredString(minecraft.font,
                Component.translatable("gui.create_gmf.world_measurement.cancel"),
                graphics.guiWidth() / 2, top + 40, 0xFFB8C1CC);
        graphics.fill(left + 4, top + 49, left + panelWidth - 4, top + 51, 0xFF303843);
        graphics.fill(left + 4, top + 49,
                left + 4 + (int) ((panelWidth - 8) * Math.max(0, Math.min(1, progress))), top + 51, 0xFF6AAE65);
    }

    private static Component number(double value) {
        return Component.literal(String.format(Locale.ROOT, "%.1f", value));
    }
}
