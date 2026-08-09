package dev.creategmf.gui;

import java.util.Locale;

import dev.creategmf.hardware.HardwareDetector;
import dev.creategmf.hardware.HardwareSnapshot;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfHardwareScreen extends GmfScreen {
    private HardwareSnapshot hardware;

    public GmfHardwareScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.hardware"), parent);
    }

    @Override
    protected void init() {
        hardware = HardwareDetector.capture();
        addBackButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        if (hardware != null) {
            int x = width / 2 - 128;
            int y = 46;
            draw(graphics, x, y, "gui.create_gmf.hardware.cpu", Component.literal(hardware.cpuName()));
            draw(graphics, x, y + 14, "gui.create_gmf.hardware.threads", Component.literal(Integer.toString(hardware.logicalProcessors())));
            draw(graphics, x, y + 28, "gui.create_gmf.hardware.gpu", Component.literal(hardware.gpuName()));
            draw(graphics, x, y + 42, "gui.create_gmf.hardware.ram", bytes(hardware.systemRamBytes()));
            draw(graphics, x, y + 56, "gui.create_gmf.hardware.heap", bytes(hardware.heapMaxBytes()));
            draw(graphics, x, y + 70, "gui.create_gmf.hardware.resolution",
                    Component.literal(hardware.windowWidth() + "x" + hardware.windowHeight()));
            draw(graphics, x, y + 84, "gui.create_gmf.hardware.distances",
                    Component.literal(hardware.renderDistance() + "/" + hardware.simulationDistance()));
            draw(graphics, x, y + 98, "gui.create_gmf.hardware.flywheel", Component.literal(hardware.flywheelBackend()));
            draw(graphics, x, y + 112, "gui.create_gmf.hardware.shaders", Component.literal(hardware.shaderLoader()));
            draw(graphics, x, y + 126, "gui.create_gmf.hardware.renderer", Component.literal(hardware.rendererMod()));
            draw(graphics, x, y + 140, "gui.create_gmf.hardware.vram",
                    hardware.vramBytes() < 0 ? Component.translatable("gui.create_gmf.unavailable") : bytes(hardware.vramBytes()));
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void draw(GuiGraphics graphics, int x, int y, String key, Component value) {
        graphics.drawString(font, Component.translatable(key, value), x, y, 0xFFD0D8E0);
    }

    private static Component bytes(long value) {
        if (value < 0) {
            return Component.translatable("gui.create_gmf.unavailable");
        }
        return Component.translatable("unit.create_gmf.gibibytes",
                Component.literal(String.format(Locale.ROOT, "%.1f", value / 1_073_741_824.0)));
    }
}
