package dev.creategmf.legacy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/** Real PoseStack drawing adapter for Minecraft 1.18.2/1.19.2 (pre-GuiGraphics). */
public final class GmfGraphics {
    private final PoseStack pose;
    public GmfGraphics(PoseStack pose) { this.pose = pose; }
    public PoseStack pose() { return pose; }
    public int guiWidth() { return Minecraft.getInstance().getWindow().getGuiScaledWidth(); }
    public int guiHeight() { return Minecraft.getInstance().getWindow().getGuiScaledHeight(); }
    public void fill(int x1, int y1, int x2, int y2, int color) { GuiComponent.fill(pose, x1, y1, x2, y2, color); }
    public int drawString(Font font, Component text, int x, int y, int color) { return font.drawShadow(pose, text, x, y, color); }
    public int drawString(Font font, String text, int x, int y, int color) { return font.drawShadow(pose, text, x, y, color); }
    public int drawString(Font font, FormattedCharSequence text, int x, int y, int color) { return font.drawShadow(pose, text, x, y, color); }
    public void drawCenteredString(Font font, Component text, int x, int y, int color) { drawString(font, text, x - font.width(text) / 2, y, color); }
    public void drawCenteredString(Font font, String text, int x, int y, int color) { drawString(font, text, x - font.width(text) / 2, y, color); }
    public void drawCenteredString(Font font, FormattedCharSequence text, int x, int y, int color) { drawString(font, text, x - font.width(text) / 2, y, color); }
    public void drawWordWrap(Font font, Component text, int x, int y, int width, int color) {
        for (var line : font.split(text, width)) { drawString(font, line, x, y, color); y += font.lineHeight; }
    }
    public void enableScissor(int x1, int y1, int x2, int y2) {
        var window = Minecraft.getInstance().getWindow();
        double scale = window.getGuiScale();
        RenderSystem.enableScissor((int) Math.floor(x1 * scale),
                (int) Math.floor(window.getHeight() - y2 * scale),
                Math.max(0, (int) Math.ceil((x2 - x1) * scale)),
                Math.max(0, (int) Math.ceil((y2 - y1) * scale)));
    }
    public void disableScissor() { RenderSystem.disableScissor(); }
    public void blit(ResourceLocation texture, int x, int y, int width, int height, float u, float v,
            int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
        RenderSystem.setShaderTexture(0, texture);
        GuiComponent.blit(pose, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }
    public void renderTooltip(Font font, List<FormattedCharSequence> lines, int mouseX, int mouseY) {
        if (lines.isEmpty()) return;
        int width = lines.stream().mapToInt(font::width).max().orElse(0);
        int height = lines.size() * (font.lineHeight + 2) - 2;
        int x = Math.max(4, Math.min(mouseX + 12, guiWidth() - width - 8));
        int y = Math.max(4, Math.min(mouseY - 12, guiHeight() - height - 8));
        pose.pushPose();
        pose.translate(0, 0, 400);
        fill(x - 4, y - 4, x + width + 4, y + height + 4, 0xF0100010);
        fill(x - 4, y - 4, x + width + 4, y - 3, 0xFF604080);
        fill(x - 4, y + height + 3, x + width + 4, y + height + 4, 0xFF604080);
        for (var line : lines) { drawString(font, line, x, y, 0xFFFFFFFF); y += font.lineHeight + 2; }
        pose.popPose();
    }
}
