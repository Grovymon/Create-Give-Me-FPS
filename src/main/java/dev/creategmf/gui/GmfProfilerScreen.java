package dev.creategmf.gui;

import java.util.List;
import java.util.Locale;

import dev.creategmf.diagnostics.CreateSceneScanner;
import dev.creategmf.diagnostics.MechanismLoad;
import dev.creategmf.diagnostics.BlockEntityLoad;
import dev.creategmf.diagnostics.SceneCensus;
import dev.creategmf.optimization.belts.BeltShadowCounters;
import dev.creategmf.optimization.belts.BeltShadowOptimizer;
import dev.creategmf.profiler.FrameStatistics;
import dev.creategmf.profiler.FrameTimeCollector;
import dev.creategmf.profiler.MemoryMetricsCollector;
import dev.creategmf.profiler.ServerTickCollector;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfProfilerScreen extends GmfScreen {
    private SceneCensus scene = SceneCensus.EMPTY;

    public GmfProfilerScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.profiler_title"), parent);
    }

    @Override
    protected void init() {
        scene = CreateSceneScanner.captureNearby();
        addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.refresh"), button ->
                        scene = CreateSceneScanner.captureNearby())
                .bounds(width / 2 - 100, 220, 200, 20).build());
        addBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        FrameStatistics frames = FrameTimeCollector.INSTANCE.snapshot();
        var memory = MemoryMetricsCollector.INSTANCE.snapshot();
        BeltShadowCounters counters = BeltShadowOptimizer.counters();
        int x = width / 2 - 125;
        int y = 48;
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.frame_measured",
                number(frames.averageMilliseconds()), frames.samples()), x, y, 0xFFFFFFFF);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.mspt_measured",
                number(ServerTickCollector.averageMspt()), ServerTickCollector.sampleCount()), x, y + 14, 0xFFD0D8E0);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.heap_measured",
                bytes(memory.heapUsedBytes()), bytes(memory.heapMaxBytes())), x, y + 28, 0xFFD0D8E0);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.create_be_estimated",
                scene.createBlockEntities()), x, y + 48, 0xFFB8C1CC);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.belts_estimated",
                scene.beltControllers(), scene.transportedItems()), x, y + 62, 0xFFB8C1CC);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.contraptions_estimated",
                scene.contraptions()), x, y + 76, 0xFFB8C1CC);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.shadow_counters",
                counters.attempted(), counters.rendered(), counters.skipped()), x, y + 96, 0xFFE4BB67);
        List<MechanismLoad> heaviest = scene.topMechanisms(3);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.heavy_mechanisms_estimated"),
                x, y + 116, 0xFFE4BB67);
        for (int index = 0; index < heaviest.size(); index++) {
            MechanismLoad load = heaviest.get(index);
            graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.mechanism_load_entry",
                    Component.translatable(load.group().translationKey()), load.objects()), x, y + 130 + index * 12,
                    0xFFB8C1CC);
        }
        List<BlockEntityLoad> specific = scene.topBlockEntityTypes(3);
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.specific_mechanisms_estimated"),
                x, y + 174, 0xFFE4BB67);
        for (int index = 0; index < specific.size(); index++) {
            BlockEntityLoad load = specific.get(index);
            graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.block_entity_load_entry",
                    Component.literal(load.typeName()), load.objects()), x, y + 188 + index * 12, 0xFFB8C1CC);
        }
        graphics.drawString(font, Component.translatable("gui.create_gmf.profiler.counts_not_time"),
                x, y + 228, 0xFF9EA8B5);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component number(double value) {
        return Component.literal(String.format(Locale.ROOT, "%.2f", value));
    }

    private static Component bytes(long value) {
        if (value < 0) {
            return Component.translatable("gui.create_gmf.unavailable");
        }
        return Component.translatable("unit.create_gmf.mebibytes",
                Component.literal(String.format(Locale.ROOT, "%.1f", value / 1_048_576.0)));
    }
}
