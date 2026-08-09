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

    public GmfRecommendationScreen(Screen parent, DiagnosticsResult diagnostics) {
        super(Component.translatable("gui.create_gmf.recommended_optimization"), parent);
        this.diagnostics = diagnostics;
    }

    @Override
    protected void init() {
        recommendations = RecommendationEngine.recommendations(diagnostics);
        if (!recommendations.isEmpty()) {
            Recommendation recommendation = recommendations.getFirst();
            addRenderableWidget(Button.builder(Component.translatable("gui.create_gmf.apply"), button -> {
                RecommendationEngine.apply(recommendation);
                onClose();
            }).bounds(width / 2 - 100, 154, 200, 20).build());
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
            Recommendation recommendation = recommendations.getFirst();
            int x = width / 2 - 120;
            graphics.drawString(font, Component.translatable(recommendation.settingTranslationKey()), x, 54, 0xFFFFFFFF);
            graphics.drawString(font, Component.translatable("gui.create_gmf.old_to_new",
                    Component.literal(Integer.toString((int) recommendation.currentValue())),
                    Component.literal(Integer.toString((int) recommendation.proposedValue()))), x, 70, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.expected_performance_impact",
                    Component.translatable(recommendation.expectedImpactTranslationKey())), x, 88, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.expected_visual_impact",
                    Component.translatable(recommendation.visualImpactTranslationKey())), x, 104, 0xFFD0D8E0);
            graphics.drawString(font, Component.translatable("gui.create_gmf.confidence",
                    Component.translatable(recommendation.confidence().translationKey())), x, 120, 0xFFE4BB67);
            graphics.drawString(font, Component.translatable(recommendation.reasonTranslationKey()), x, 136, 0xFFB8C1CC);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
