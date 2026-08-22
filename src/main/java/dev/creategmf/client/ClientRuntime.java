package dev.creategmf.client;

import dev.creategmf.benchmark.OptimizationBenchmarkSession;
import dev.creategmf.benchmark.PcBenchmarkSession;
import dev.creategmf.diagnostics.DiagnosticSession;
import dev.creategmf.diagnostics.DeveloperDiagnostics;
import dev.creategmf.gui.GmfDiagnosticsScreen;
import dev.creategmf.gui.GmfSettingsScreen;
import dev.creategmf.gui.GmfOptimizationBenchmarkScreen;
import dev.creategmf.optimization.animations.DistantAnimationController;
import dev.creategmf.optimization.occlusion.CreateOcclusionController;
import dev.creategmf.profiler.FrameTimeCollector;
import dev.creategmf.profiler.MemoryMetricsCollector;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

public final class ClientRuntime {
    private static int memorySampleTicks;

    private ClientRuntime() {
    }

    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        DistantAnimationController.nextFrame();
        long frameNanos = FrameTimeCollector.INSTANCE.recordFrame(System.nanoTime());
        PcBenchmarkSession.INSTANCE.onFrame(frameNanos);
        DiagnosticSession.INSTANCE.onFrame(frameNanos);
        DeveloperDiagnostics.INSTANCE.onFrame(frameNanos);
        OptimizationBenchmarkSession.INSTANCE.onFrame(frameNanos);
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (ClientKeyMappings.OPEN_MENU.consumeClick()) {
            if (DiagnosticSession.INSTANCE.isActive()) {
                DiagnosticSession.INSTANCE.cancel();
                minecraft.setScreen(new GmfDiagnosticsScreen(null));
            } else if (OptimizationBenchmarkSession.INSTANCE.isActive()) {
                OptimizationBenchmarkSession.INSTANCE.cancel();
                minecraft.setScreen(new GmfOptimizationBenchmarkScreen(null));
            } else {
                minecraft.setScreen(new GmfSettingsScreen(minecraft.screen));
            }
        }

        if (++memorySampleTicks >= 20) {
            memorySampleTicks = 0;
            MemoryMetricsCollector.INSTANCE.sample();
        }
        DistantAnimationController.tick();
        CreateOcclusionController.onClientTick();
        DeveloperDiagnostics.INSTANCE.onClientTick();
        PcBenchmarkSession.INSTANCE.tick();
        if (DiagnosticSession.INSTANCE.isActive() && minecraft.screen != null) {
            DiagnosticSession.INSTANCE.cancel();
        } else {
            DiagnosticSession.State before = DiagnosticSession.INSTANCE.state();
            DiagnosticSession.INSTANCE.tick();
            if (before != DiagnosticSession.State.COMPLETE
                    && DiagnosticSession.INSTANCE.state() == DiagnosticSession.State.COMPLETE
                    && minecraft.screen == null) {
                minecraft.setScreen(new GmfDiagnosticsScreen(null));
            }
        }
        if (OptimizationBenchmarkSession.INSTANCE.isActive() && minecraft.screen != null) {
            OptimizationBenchmarkSession.INSTANCE.cancel();
        } else {
            OptimizationBenchmarkSession.State before = OptimizationBenchmarkSession.INSTANCE.state();
            OptimizationBenchmarkSession.INSTANCE.tick();
            if (before != OptimizationBenchmarkSession.State.COMPLETE
                    && OptimizationBenchmarkSession.INSTANCE.state() == OptimizationBenchmarkSession.State.COMPLETE
                    && minecraft.screen == null) {
                minecraft.setScreen(new GmfOptimizationBenchmarkScreen(null));
            }
        }
    }
}
