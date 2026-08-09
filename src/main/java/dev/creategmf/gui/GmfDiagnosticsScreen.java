package dev.creategmf.gui;

import java.util.Locale;

import dev.creategmf.diagnostics.DiagnosticSession;
import dev.creategmf.diagnostics.DiagnosticsResult;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfDiagnosticsScreen extends GmfScreen {
    private Button startButton;
    private Button cancelButton;
    private Button settingsButton;

    public GmfDiagnosticsScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.find_lag_source"), parent);
    }

    @Override
    protected void init() {
        int x = width / 2 - 110;
        startButton = addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.start_diagnostics"),
                        button -> {
                            DiagnosticSession.INSTANCE.start();
                            minecraft.setScreen(null);
                        })
                .bounds(x, 88, 220, 20).build());
        cancelButton = addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.cancel"), button -> {
                    DiagnosticSession.INSTANCE.cancel();
                    updateButtons();
                }).bounds(x, 112, 220, 20).build());
        settingsButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.create_gmf.open_mechanism_settings"), button ->
                                minecraft.setScreen(new GmfSettingsScreen(this)))
                .bounds(x, height - 52, 220, 20).build());
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
        var state = DiagnosticSession.INSTANCE.state();
        boolean running = DiagnosticSession.INSTANCE.isActive();
        startButton.active = !running;
        cancelButton.active = running;
        settingsButton.active = state == DiagnosticSession.State.COMPLETE;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        DiagnosticSession session = DiagnosticSession.INSTANCE;
        graphics.drawCenteredString(font,
                Component.translatable("gui.create_gmf.diagnostics.state." + session.state().name().toLowerCase(Locale.ROOT)),
                width / 2, 44, 0xFFFFFFFF);
        if (session.state() == DiagnosticSession.State.IDLE) {
            graphics.drawCenteredString(font, Component.translatable("gui.create_gmf.diagnostics.move_to_lag"),
                    width / 2, 58, 0xFFD0D8E0);
            graphics.drawCenteredString(font, Component.translatable("gui.create_gmf.diagnostics.look_at_scene"),
                    width / 2, 70, 0xFFD0D8E0);
        }
        drawProgress(graphics, session.progress(), 138);

        DiagnosticsResult result = session.result();
        if (result != null) {
            int x = width / 2 - 118;
            graphics.drawString(font, Component.translatable("gui.create_gmf.metric.fps",
                    number(result.frames().averageFps())), x, 153, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.metric.one_percent_low",
                    result.frames().onePercentLow() > 0 ? number(result.frames().onePercentLow())
                            : Component.translatable("gui.create_gmf.unavailable")), x, 166, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.primary_bottleneck",
                    Component.translatable(result.primaryBottleneck().translationKey())), x, 179, 0xFFE4BB67);
            graphics.drawString(font, Component.translatable("gui.create_gmf.evidence_and_confidence",
                    Component.translatable(result.evidence().translationKey()),
                    Component.translatable(result.confidence().translationKey())), x, 192, 0xFFB8C1CC);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Component number(double value) {
        return Component.literal(String.format(Locale.ROOT, "%.1f", value));
    }
}
