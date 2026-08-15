package dev.creategmf.gui;

import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.PcProfile;
import dev.creategmf.optimization.animations.DistantAnimationController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** A focused screen so the profile list never overlays the settings controls. */
public final class GmfProfileSelectionScreen extends GmfScreen {
    private static final PcProfile[] PRESETS = {
            PcProfile.POTATO, PcProfile.LOW, PcProfile.MEDIUM,
            PcProfile.ABOVE_AVERAGE, PcProfile.HIGH, PcProfile.ULTRA
    };

    public GmfProfileSelectionScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.profile_dialog"), parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF090B0E);
        int cardWidth = Math.min(360, width - 36);
        int left = (width - cardWidth) / 2;
        int optionWidth = cardWidth - 32;
        int top = Math.max(20, (height - 250) / 2);
        int bottom = top + 250;
        graphics.fill(left, top, left + cardWidth, bottom, 0xFF171717);
        outline(graphics, left, top, left + cardWidth, bottom, 0xFFE4BB67);
        graphics.drawCenteredString(font, title, width / 2, top + 18, 0xFFE4BB67);
        graphics.drawCenteredString(font, Component.translatable("gui.create_gmf.profile_dialog_hint"), width / 2,
                top + 37, 0xFFA9A196);

        int y = top + 58;
        for (PcProfile preset : PRESETS) {
            boolean selected = GmfConfig.CLIENT.pcProfile.get() == preset;
            boolean hover = mouseX >= left + 16 && mouseX < left + 16 + optionWidth && mouseY >= y && mouseY < y + 24;
            int fill = selected ? 0xFF735329 : hover ? 0xFF302A22 : 0xFF1C1C1C;
            graphics.fill(left + 16, y, left + 16 + optionWidth, y + 24, fill);
            outline(graphics, left + 16, y, left + 16 + optionWidth, y + 24,
                    selected || hover ? 0xFFE4BB67 : 0xFF5F472A);
            graphics.drawCenteredString(font, Component.translatable(preset.translationKey()), width / 2, y + 8,
                    selected ? 0xFFFFE0A2 : 0xFFE2DDD5);
            y += 28;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int cardWidth = Math.min(360, width - 36);
        int left = (width - cardWidth) / 2;
        int top = Math.max(20, (height - 250) / 2);
        int y = top + 58;
        for (PcProfile preset : PRESETS) {
            if (mouseX >= left + 16 && mouseX < left + cardWidth - 16 && mouseY >= y && mouseY < y + 24) {
                GmfConfig.applyProfile(preset);
                GmfConfig.save();
                DistantAnimationController.refreshLoadedKinetics();
                if (minecraft != null) minecraft.setScreen(parent);
                return true;
            }
            y += 28;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }
}
