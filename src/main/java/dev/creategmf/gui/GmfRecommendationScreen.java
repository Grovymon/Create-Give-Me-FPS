package dev.creategmf.gui;

import java.util.List;

import dev.creategmf.diagnostics.DiagnosticsResult;
import dev.creategmf.recommendation.Recommendation;
import dev.creategmf.recommendation.RecommendationEngine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfRecommendationScreen extends GmfScreen {
    private final DiagnosticsResult diagnostics;
    private List<Recommendation> recommendations = List.of();
    private int selected;
    private int scroll;
    private int contentHeight;

    public GmfRecommendationScreen(Screen parent, DiagnosticsResult diagnostics) {
        super(Component.translatable("gui.create_gmf.recommended_optimization"), parent);
        this.diagnostics = diagnostics;
    }

    @Override
    protected void init() {
        recommendations = RecommendationEngine.recommendations(diagnostics);
        if (!recommendations.isEmpty()) {
            Recommendation recommendation = recommendations.get(Math.min(selected, recommendations.size() - 1));
            addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.next_recommendation"), button -> {
                selected = (selected + 1) % recommendations.size();
                scroll = 0;
                rebuildWidgets();
            }).bounds(width / 2 - 100, 42, 200, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.apply"), button -> {
                RecommendationEngine.apply(recommendation);
                onClose();
            }).bounds(width / 2 - 100, height - 54, 200, 20).build()).active =
                    recommendation.action() != Recommendation.Action.RENDERER_EXPERIMENT;
        }
        addBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        if (recommendations.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("gui.create_gmf.no_safe_recommendation"),
                    width / 2, 82, 0xFFD0D8E0);
        } else {
            Recommendation recommendation = recommendations.get(selected);
            int w = Math.max(1, Math.min(360, width - 40)), x = width / 2 - w / 2, y = 70 - scroll;
            java.util.List<Component> lines = java.util.List.of(Component.translatable(recommendation.settingTranslationKey()),
                    Component.translatable("gui.create_gmf.old_to_new", (int) recommendation.currentValue(),
                            (int) recommendation.proposedValue()),
                    Component.translatable("gui.create_gmf.expected_performance_impact",
                            Component.translatable(recommendation.expectedImpactTranslationKey())),
                    Component.translatable("gui.create_gmf.expected_visual_impact",
                            Component.translatable(recommendation.visualImpactTranslationKey())),
                    Component.translatable("gui.create_gmf.confidence", Component.translatable(recommendation.confidence().translationKey())),
                    Component.translatable(recommendation.reasonTranslationKey(), recommendation.evidenceCount()));
            graphics.enableScissor(x, 68, x + w, height - 60);
            for (Component line : lines) {
                graphics.drawWordWrap(font, line, x, y, w, 0xFFB8C1CC);
                y += font.split(line, w).size() * font.lineHeight + 4;
            }
            graphics.disableScissor();
            contentHeight = y + scroll - 70;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll = Math.clamp(scroll - (int) (dy * 18), 0,
                Math.max(0, contentHeight - Math.max(0, height - 130)));
        return true;
    }
}
