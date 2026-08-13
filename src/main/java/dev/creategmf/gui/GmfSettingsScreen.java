package dev.creategmf.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.creategmf.config.CreateParticleMode;
import dev.creategmf.config.DistantAnimationMode;
import dev.creategmf.config.FlywheelBackendMode;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.config.PcProfile;
import dev.creategmf.client.RendererRestartTracker;
import dev.creategmf.integration.FlywheelBackendController;
import dev.creategmf.integration.ShaderStatusDetector;
import dev.creategmf.optimization.animations.DistantAnimationController;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The settings screen intentionally uses lightweight hand-drawn controls instead
 * of a grid of vanilla buttons.  Every control still writes the normal GMF config.
 */
public final class GmfSettingsScreen extends GmfScreen {
    private static final int MAX_ANIMATION_DISTANCE = 256;
    private static final int[] REDUCED_ANIMATION_FPS = {1, 2, 5, 10, 15, 20, 30};
    private static final PcProfile[] PRESETS = {
            PcProfile.POTATO, PcProfile.LOW, PcProfile.MEDIUM,
            PcProfile.ABOVE_AVERAGE, PcProfile.HIGH, PcProfile.ULTRA
    };
    private static final String FEEDBACK_URL = "https://github.com/Grovymon/Create-Give-Me-FPS/issues";
    private static final ResourceLocation MOD_ICON = ResourceLocation.fromNamespaceAndPath("create_gmf", "textures/gui/logo.png");

    private final List<HitArea> hitAreas = new ArrayList<>();
    private Page page = Page.MECHANISMS;
    private EditBox searchBox;
    private String search = "";
    private int panelLeft;
    private int panelRight;
    private int contentLeft;
    private int contentRight;
    private boolean individualMechanismsOpen;
    private int profileButtonX;
    private int profileButtonY;
    private int profileButtonWidth;
    private int scrollOffset;
    private int maxScroll;
    private int drawnContentBottom;
    private boolean drawingScrollableContent;

    public GmfSettingsScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.rendering_settings"), parent);
    }

    @Override
    protected void init() {
        rebuildSearchBox();
    }

    private void rebuildSearchBox() {
        if (searchBox != null) {
            removeWidget(searchBox);
            searchBox = null;
        }
        if (page != Page.MECHANISMS || !individualMechanismsOpen) {
            return;
        }
        searchBox = new EditBox(font, 0, 0, 1, 18, Component.translatable("gui.create_gmf.settings.search"));
        searchBox.setHint(Component.translatable("gui.create_gmf.settings.search_hint"));
        searchBox.setValue(search);
        searchBox.setResponder(value -> search = value);
        addRenderableWidget(searchBox);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        drawShell(graphics);
        hitAreas.clear();
        drawTopBar(graphics, mouseX, mouseY);
        drawNavigation(graphics, mouseX, mouseY);
        beginContent();
        int viewportBottom = contentViewportBottom();
        drawnContentBottom = 78;
        drawingScrollableContent = true;
        graphics.enableScissor(contentLeft, 78, contentRight, viewportBottom);
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scrollOffset, 0);
        int scrolledMouseY = mouseY + scrollOffset;
        switch (page) {
            case MECHANISMS -> {
                if (individualMechanismsOpen) drawIndividualMechanisms(graphics, mouseX, scrolledMouseY);
                else drawMechanisms(graphics, mouseX, scrolledMouseY);
            }
            case PARTICLES -> drawParticles(graphics, mouseX, scrolledMouseY);
            case FLYWHEEL -> drawFlywheel(graphics, mouseX, scrolledMouseY);
            case SHADERS -> drawShaders(graphics, mouseX, scrolledMouseY);
            case OTHER -> drawOther(graphics, mouseX, scrolledMouseY);
            case FEEDBACK -> drawFeedback(graphics, mouseX, scrolledMouseY);
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        drawingScrollableContent = false;
        maxScroll = Math.max(0, drawnContentBottom - viewportBottom);
        scrollOffset = Math.clamp(scrollOffset, 0, maxScroll);
        drawScrollbar(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawShell(GuiGraphics graphics) {
        if (minecraft != null && minecraft.level != null) {
            graphics.fill(0, 0, width, height, 0x9A050709);
        } else {
            graphics.fill(0, 0, width, height, 0xFF090B0E);
        }
        int panelWidth = Math.min(1120, width - 32);
        panelLeft = (width - panelWidth) / 2;
        panelRight = panelLeft + panelWidth;
        graphics.fill(panelLeft, 8, panelRight, height - 8, 0xF0141517);
        outline(graphics, panelLeft, 8, panelRight, height - 8, 0xFF8C6331);
    }

    private void drawTopBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int top = 16;
        int bottom = 66;
        graphics.fill(panelLeft + 8, top, panelRight - 8, bottom, 0xFF1B1B1B);
        outline(graphics, panelLeft + 8, top, panelRight - 8, bottom, 0xFF604526);
        int side = sidebarWidth();
        graphics.blit(MOD_ICON, panelLeft + 20, top + 7, 36, 36,
                0, 0, 64, 64, 64, 64);
        int x = panelLeft + side + 18;
        Component profileLabel = Component.translatable("gui.create_gmf.profile_label");
        graphics.drawString(font, profileLabel, x, top + 20, 0xFFE4BB67);
        x += font.width(profileLabel) + 8;
        int resetWidth = 92;
        profileButtonX = x;
        profileButtonY = top + 10;
        profileButtonWidth = Math.max(88, Math.min(150, panelRight - 18 - resetWidth - 10 - x));
        drawDropdownValue(graphics, profileButtonX, profileButtonY, profileButtonWidth,
                Component.translatable(GmfConfig.CLIENT.pcProfile.get().translationKey()), false,
                mouseX, mouseY);
        addHitArea(profileButtonX, profileButtonY, profileButtonWidth, 28,
                () -> minecraft.setScreen(new GmfProfileSelectionScreen(this)));
        drawAction(graphics, panelRight - 18 - resetWidth, top + 10, resetWidth,
                Component.translatable("gui.create_gmf.reset_short"), false,
                mouseX, mouseY);
        addHitArea(panelRight - 18 - resetWidth, top + 10, resetWidth, 28, this::reset);
    }

    private void drawNavigation(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = panelLeft + 14;
        int w = sidebarWidth() - 16;
        int y = 78;
        int available = Math.max(120, contentViewportBottom() - y);
        int step = Math.max(22, Math.min(38, available / Page.values().length));
        int buttonHeight = Math.max(19, step - 4);
        for (Page option : Page.values()) {
            boolean selected = option == page;
            drawAction(graphics, x, y, w, buttonHeight, Component.translatable(option.key), selected, mouseX, mouseY);
            addHitArea(x, y, w, buttonHeight, () -> switchPage(option));
            y += step;
        }
    }

    private void drawMechanisms(GuiGraphics graphics, int mouseX, int mouseY) {
        beginContent();
        int cardWidth = contentRight - contentLeft;
        int left = contentLeft;
        int y = 78;
        int cardTop = y;
        y = beginCard(graphics, left, y, cardWidth, Component.translatable("gui.create_gmf.card.general"));
        y = toggleRow(graphics, left, y, cardWidth, Component.translatable("gui.create_gmf.all_mechanisms"),
                allMechanismsEnabled(), this::toggleAllMechanisms, mouseX, mouseY);
        y = sliderRow(graphics, left, y, cardWidth, Component.translatable("config.create_gmf.animation_distance_slider"),
                GmfConfig.CLIENT.distantAnimationDistance.get().intValue(), 0, MAX_ANIMATION_DISTANCE,
                value -> {
                    GmfConfig.CLIENT.distantAnimationDistance.set((double) value);
                    customAndRefresh();
                }, mouseX, mouseY);
        y = cycleRow(graphics, left, y, cardWidth, Component.translatable("gui.create_gmf.distant_animation_mode"),
                Component.translatable(GmfConfig.CLIENT.distantAnimationMode.get().translationKey()), () -> {
                    GmfConfig.CLIENT.distantAnimationMode.set(next(DistantAnimationMode.values(),
                            GmfConfig.CLIENT.distantAnimationMode.get()));
                    customAndRefresh();
                }, mouseX, mouseY);
        y = cycleRow(graphics, left, y, cardWidth, Component.translatable("config.create_gmf.reduced_animation_fps"),
                Component.translatable("unit.create_gmf.fps", GmfConfig.CLIENT.reducedAnimationFps.get()), () -> {
                    GmfConfig.CLIENT.reducedAnimationFps.set(next(REDUCED_ANIMATION_FPS,
                            GmfConfig.CLIENT.reducedAnimationFps.get()));
                    customAndRefresh();
                }, mouseX, mouseY);
        endCard(graphics, left, cardTop, cardWidth, y);

        y += 12;
        cardTop = y;
        y = beginCard(graphics, left, y, cardWidth, Component.translatable("gui.create_gmf.card.animations"));
        drawAction(graphics, left + 12, y, Math.min(230, cardWidth - 24),
                Component.translatable("gui.create_gmf.open_individual_mechanisms"), false, mouseX, mouseY);
        addHitArea(left + 12, y, Math.min(230, cardWidth - 24), 28, () -> {
            individualMechanismsOpen = true;
            scrollOffset = 0;
            rebuildSearchBox();
        });
        y += 36;
        endCard(graphics, left, cardTop, cardWidth, y);

        y += 12;
        cardTop = y;
        y = beginCard(graphics, left, y, cardWidth, Component.translatable("gui.create_gmf.card.belts"));
        y = toggleRow(graphics, left, y, cardWidth,
                Component.translatable("config.create_gmf.render_transported_belt_items"),
                GmfConfig.CLIENT.renderTransportedBeltItems.get(), () -> {
                    GmfConfig.CLIENT.renderTransportedBeltItems.set(!GmfConfig.CLIENT.renderTransportedBeltItems.get());
                    customAndRefresh();
                }, mouseX, mouseY);
        y = toggleRow(graphics, left, y, cardWidth, Component.translatable("config.create_gmf.belt_item_shadows"),
                GmfConfig.CLIENT.beltItemShadowOptimization.get(), () -> {
                    GmfConfig.CLIENT.beltItemShadowOptimization.set(!GmfConfig.CLIENT.beltItemShadowOptimization.get());
                    customAndSave();
                }, mouseX, mouseY);
        y += 8;
        y = toggleRow(graphics, left, y, cardWidth,
                Component.translatable("config.create_gmf.crushing_output_rendering"),
                GmfConfig.CLIENT.crushingOutputRendering.get(), () -> {
                    GmfConfig.CLIENT.crushingOutputRendering.set(!GmfConfig.CLIENT.crushingOutputRendering.get());
                    customAndSave();
                }, mouseX, mouseY);
        endCard(graphics, left, cardTop, cardWidth, y);

    }

    private void drawIndividualMechanisms(GuiGraphics graphics, int mouseX, int mouseY) {
        beginContent();
        int width = contentRight - contentLeft;
        int y = 78;
        int top = y;
        y = beginCard(graphics, contentLeft, y, width, Component.translatable("gui.create_gmf.card.mechanism_list"));
        drawAction(graphics, contentLeft + 12, y, 110, Component.translatable("gui.create_gmf.back"), false, mouseX, mouseY);
        addHitArea(contentLeft + 12, y, 110, 28, () -> {
            individualMechanismsOpen = false;
            scrollOffset = 0;
            rebuildSearchBox();
        });
        y += 36;
        if (searchBox != null) {
            searchBox.setX(contentLeft + 12);
            searchBox.setY(y - scrollOffset);
            searchBox.setWidth(width - 24);
            searchBox.setHeight(18);
            searchBox.visible = searchBox.getY() >= 78 && searchBox.getY() + 18 <= contentViewportBottom();
            y += 28;
        }
        int found = 0;
        for (MechanismAnimationGroup group : MechanismAnimationGroup.values()) {
            if (!matches(group)) continue;
            y = toggleRow(graphics, contentLeft, y, width, Component.translatable(group.translationKey()),
                    GmfConfig.CLIENT.mechanismAnimations.get(group).get(), () -> {
                        var value = GmfConfig.CLIENT.mechanismAnimations.get(group);
                        value.set(!value.get());
                        customAndRefresh();
                    }, mouseX, mouseY);
            found++;
        }
        if (found == 0) {
            y = drawWrappedText(graphics, Component.translatable("gui.create_gmf.settings.search_empty", search),
                    contentLeft + 12, y + 4, width - 24, 0xFFBDB4A6) + 8;
        }
        endCard(graphics, contentLeft, top, width, y);
    }

    private void drawParticles(GuiGraphics graphics, int mouseX, int mouseY) {
        beginContent();
        int w = contentRight - contentLeft;
        int y = beginCard(graphics, contentLeft, 78, w, Component.translatable("gui.create_gmf.card.particles"));
        y = cycleRow(graphics, contentLeft, y, w, Component.translatable("config.create_gmf.create_particles"),
                Component.translatable(GmfConfig.CLIENT.createParticleMode.get().translationKey()), () -> {
                    GmfConfig.CLIENT.createParticleMode.set(next(CreateParticleMode.values(),
                            GmfConfig.CLIENT.createParticleMode.get()));
                    customAndSave();
                }, mouseX, mouseY);
        y = toggleRow(graphics, contentLeft, y, w, Component.translatable("config.create_gmf.filter_create_particles"),
                GmfConfig.CLIENT.filterCreateParticles.get(), () -> {
                    GmfConfig.CLIENT.filterCreateParticles.set(!GmfConfig.CLIENT.filterCreateParticles.get());
                    customAndSave();
                }, mouseX, mouseY);
        y = toggleRow(graphics, contentLeft, y, w, Component.translatable("config.create_gmf.filter_fluid_particles"),
                GmfConfig.CLIENT.filterFluidParticles.get(), () -> {
                    GmfConfig.CLIENT.filterFluidParticles.set(!GmfConfig.CLIENT.filterFluidParticles.get());
                    customAndSave();
                }, mouseX, mouseY);
        y = drawWrappedText(graphics, Component.translatable("gui.create_gmf.particles.note"), contentLeft + 12, y + 5,
                w - 24, 0xFFA9A196);
        endCard(graphics, contentLeft, 78, w, y + 8);
    }

    private void drawFlywheel(GuiGraphics graphics, int mouseX, int mouseY) {
        beginContent();
        int w = contentRight - contentLeft;
        int y = beginCard(graphics, contentLeft, 78, w, Component.translatable("gui.create_gmf.card.flywheel"));
        y = cycleRow(graphics, contentLeft, y, w, Component.translatable("gui.create_gmf.flywheel_renderer"),
                Component.translatable(GmfConfig.CLIENT.flywheelBackend.get().translationKey()), () -> {
                    FlywheelBackendMode next = next(FlywheelBackendMode.values(), GmfConfig.CLIENT.flywheelBackend.get());
                    GmfConfig.CLIENT.flywheelBackend.set(next);
                    GmfConfig.save();
                    FlywheelBackendController.apply(next);
                  }, mouseX, mouseY);
        y = toggleRow(graphics, contentLeft, y, w, Component.translatable("config.create_gmf.accelerated_renderer"),
                GmfConfig.CLIENT.acceleratedRenderer.get(), () -> {
                    GmfConfig.CLIENT.acceleratedRenderer.set(!GmfConfig.CLIENT.acceleratedRenderer.get());
                    customAndSave();
                }, mouseX, mouseY);
        y = drawWrappedText(graphics, Component.translatable("gui.create_gmf.accelerated_renderer_hint"),
                contentLeft + 12, y + 5, w - 24, 0xFFA9A196);
        if (RendererRestartTracker.isRestartRequired()) {
            y = drawWrappedText(graphics, Component.translatable("gui.create_gmf.settings.flywheel_hint"), contentLeft + 12,
                    y + 5, w - 24, 0xFFFF5B52);
        }
        endCard(graphics, contentLeft, 78, w, y + 8);
    }

    private void drawShaders(GuiGraphics graphics, int mouseX, int mouseY) {
        beginContent();
        int w = contentRight - contentLeft;
        int y = beginCard(graphics, contentLeft, 78, w, Component.translatable("gui.create_gmf.card.shaders"));
        boolean active = ShaderStatusDetector.isShaderPackActive();
        graphics.drawString(font, Component.translatable(active ? "gui.create_gmf.shaders.active" : "gui.create_gmf.shaders.inactive"),
                contentLeft + 12, y + 8, active ? 0xFFE4BB67 : 0xFF79B96B);
        y = drawWrappedText(graphics, Component.translatable("gui.create_gmf.shaders.note"), contentLeft + 12, y + 30,
                w - 24, 0xFFA9A196);
        endCard(graphics, contentLeft, 78, w, y + 8);
    }

    private void drawOther(GuiGraphics graphics, int mouseX, int mouseY) {
        beginContent();
        int w = contentRight - contentLeft;
        int y = beginCard(graphics, contentLeft, 78, w, Component.translatable("gui.create_gmf.card.other"));
        y = toggleRow(graphics, contentLeft, y, w, Component.translatable("config.create_gmf.enable"),
                GmfConfig.CLIENT.enabled.get(), () -> {
                    GmfConfig.CLIENT.enabled.set(!GmfConfig.CLIENT.enabled.get());
                    customAndRefresh();
                }, mouseX, mouseY);
        endCard(graphics, contentLeft, 78, w, y);
    }

    private void drawFeedback(GuiGraphics graphics, int mouseX, int mouseY) {
        beginContent();
        int w = contentRight - contentLeft;
        int y = beginCard(graphics, contentLeft, 78, w, Component.translatable("gui.create_gmf.card.feedback"));
        y = drawWrappedText(graphics, Component.translatable("gui.create_gmf.feedback.description"), contentLeft + 12,
                y + 6, w - 24, 0xFFE4E0D8) + 8;
        drawAction(graphics, contentLeft + 12, y, 180, Component.translatable("gui.create_gmf.feedback.github"), false,
                mouseX, mouseY);
        addHitArea(contentLeft + 12, y, 180, 28, () -> Util.getPlatform().openUri(FEEDBACK_URL));
        endCard(graphics, contentLeft, 78, w, y + 40);
    }

    private void beginContent() {
        contentLeft = panelLeft + sidebarWidth() + 18;
        contentRight = panelRight - 18;
    }

    private int contentViewportBottom() {
        return Math.max(96, height - 18);
    }

    private int sidebarWidth() {
        return Math.min(260, Math.max(130, (panelRight - panelLeft) / 5));
    }

    private int beginCard(GuiGraphics graphics, int x, int y, int width, Component title) {
        graphics.fill(x, y, x + width, y + 34, 0xFF222120);
        outline(graphics, x, y, x + width, y + 34, 0xFF5F472A);
        graphics.drawString(font, trim(title, width - 24), x + 12, y + 12, 0xFFE4BB67);
        return y + 42;
    }

    private void endCard(GuiGraphics graphics, int x, int top, int width, int bottom) {
        outline(graphics, x, top, x + width, bottom + 8, 0xFF5F472A);
        drawnContentBottom = Math.max(drawnContentBottom, bottom + 8);
    }

    private int toggleRow(GuiGraphics graphics, int x, int y, int width, Component label, boolean value,
            Runnable action, int mouseX, int mouseY) {
        int labelBottom = drawWrappedRowLabel(graphics, x, y, width - 64, label);
        int toggleX = x + width - 48;
        drawToggle(graphics, toggleX, y, value, mouseX, mouseY);
        addHitArea(toggleX, y, 36, 16, action);
        return Math.max(y + 29, labelBottom + 10);
    }

    private int cycleRow(GuiGraphics graphics, int x, int y, int width, Component label, Component value,
            Runnable action, int mouseX, int mouseY) {
        int valueWidth = Math.min(150, Math.max(108, width / 3));
        drawRowLabel(graphics, x, y, width - valueWidth - 20, label);
        int valueX = x + width - valueWidth - 10;
        drawValue(graphics, valueX, y - 3, valueWidth, value, mouseX, mouseY);
        addHitArea(valueX, y - 3, valueWidth, 20, action);
        return y + 29;
    }

    private int sliderRow(GuiGraphics graphics, int x, int y, int width, Component label, int value, int min, int max,
            java.util.function.IntConsumer setter, int mouseX, int mouseY) {
        drawRowLabel(graphics, x, y, width - 145, label);
        int barLeft = x + width - 132;
        int barRight = x + width - 50;
        graphics.fill(barLeft, y + 7, barRight, y + 9, 0xFF594832);
        int knob = barLeft + (int) ((barRight - barLeft) * (value - min) / (double) (max - min));
        graphics.fill(knob - 3, y + 3, knob + 4, y + 13, 0xFFE4BB67);
        drawValue(graphics, x + width - 44, y - 3, 38, Component.literal(Integer.toString(value)), mouseX, mouseY);
        addHitArea(barLeft, y - 3, barRight - barLeft, 22, () -> {
            double fraction = Math.clamp((mouseX - barLeft) / (double) Math.max(1, barRight - barLeft), 0, 1);
            setter.accept((int) Math.round(min + fraction * (max - min)));
        });
        return y + 29;
    }

    private void drawRowLabel(GuiGraphics graphics, int x, int y, int width, Component label) {
        graphics.drawString(font, trim(label, Math.max(36, width - 10)), x + 12, y + 3, 0xFFE0DDD6);
    }

    private int drawWrappedRowLabel(GuiGraphics graphics, int x, int y, int width, Component label) {
        int lineY = y + 3;
        int lastLineY = lineY;
        for (var line : font.split(label, Math.max(40, width - 12))) {
            graphics.drawString(font, line, x + 12, lineY, 0xFFE0DDD6);
            lastLineY = lineY;
            lineY += font.lineHeight + 1;
        }
        return lastLineY + font.lineHeight;
    }

    private void drawToggle(GuiGraphics graphics, int x, int y, boolean value, int mouseX, int mouseY) {
        boolean hover = contains(x, y, 36, 16, mouseX, mouseY);
        graphics.fill(x, y, x + 36, y + 16, value ? 0xFF688223 : 0xFF3C3934);
        outline(graphics, x, y, x + 36, y + 16, hover ? 0xFFE4BB67 : 0xFF75572D);
        int knobX = value ? x + 21 : x + 3;
        graphics.fill(knobX, y + 3, knobX + 12, y + 13, value ? 0xFFF4E5BC : 0xFFAAA69E);
    }

    private void drawValue(GuiGraphics graphics, int x, int y, int width, Component value, int mouseX, int mouseY) {
        boolean hover = contains(x, y, width, 20, mouseX, mouseY);
        graphics.fill(x, y, x + width, y + 20, hover ? 0xFF2A2824 : 0xFF1A1A1A);
        outline(graphics, x, y, x + width, y + 20, hover ? 0xFFE4BB67 : 0xFF5F472A);
        graphics.drawCenteredString(font, trim(value, width - 8), x + width / 2, y + 6, 0xFFE0DDD6);
    }

    private void drawDropdownValue(GuiGraphics graphics, int x, int y, int width, Component value, boolean open,
            int mouseX, int mouseY) {
        boolean hover = contains(x, y, width, 28, mouseX, mouseY);
        graphics.fill(x, y, x + width, y + 28, hover || open ? 0xFF302A22 : 0xFF1C1C1C);
        outline(graphics, x, y, x + width, y + 28, hover || open ? 0xFFE4BB67 : 0xFF5F472A);
        graphics.drawString(font, trim(value, width - 28), x + 9, y + 10, 0xFFE2DDD5);
        graphics.drawString(font, Component.literal(open ? "▲" : "▼"), x + width - 18, y + 10, 0xFFE4BB67);
    }

    private int drawWrappedText(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        int lineY = y;
        for (var line : font.split(text, Math.max(20, maxWidth))) {
            graphics.drawString(font, line, x, lineY, color);
            lineY += font.lineHeight + 2;
        }
        return lineY;
    }

    private void drawScrollbar(GuiGraphics graphics) {
        if (maxScroll <= 0) return;
        int top = 78;
        int bottom = contentViewportBottom();
        int trackX = contentRight - 3;
        graphics.fill(trackX, top, trackX + 2, bottom, 0xFF3B3023);
        int viewportHeight = bottom - top;
        int contentHeight = viewportHeight + maxScroll;
        int thumbHeight = Math.max(18, viewportHeight * viewportHeight / Math.max(1, contentHeight));
        int thumbY = top + (viewportHeight - thumbHeight) * scrollOffset / Math.max(1, maxScroll);
        graphics.fill(trackX - 1, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFE4BB67);
    }

    private void drawAction(GuiGraphics graphics, int x, int y, int width, Component label, boolean selected,
            int mouseX, int mouseY) {
        drawAction(graphics, x, y, width, 28, label, selected, mouseX, mouseY);
    }

    private void drawAction(GuiGraphics graphics, int x, int y, int width, int height, Component label,
            boolean selected, int mouseX, int mouseY) {
        boolean hover = contains(x, y, width, height, mouseX, mouseY);
        int fill = selected ? 0xFF735329 : hover ? 0xFF302A22 : 0xFF1C1C1C;
        graphics.fill(x, y, x + width, y + height, fill);
        outline(graphics, x, y, x + width, y + height, selected || hover ? 0xFFE4BB67 : 0xFF4E3D2B);
        graphics.drawCenteredString(font, trim(label, width - 8), x + width / 2, y + (height - 8) / 2,
                selected ? 0xFFFFE0A2 : 0xFFE2DDD5);
    }

    private void addHitArea(int x, int y, int width, int height, Runnable action) {
        int screenY = drawingScrollableContent ? y - scrollOffset : y;
        if (drawingScrollableContent && (screenY + height <= 78 || screenY >= contentViewportBottom())) return;
        hitAreas.add(new HitArea(x, screenY, width, height, action));
    }

    private void outline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        graphics.fill(left, top, right, top + 1, color);
        graphics.fill(left, bottom - 1, right, bottom, color);
        graphics.fill(left, top, left + 1, bottom, color);
        graphics.fill(right - 1, top, right, bottom, color);
    }

    private Component trim(Component text, int maxWidth) {
        String value = text.getString();
        if (font.width(value) <= maxWidth) return text;
        return Component.literal(font.plainSubstrByWidth(value, Math.max(1, maxWidth - 6)) + "…");
    }

    private boolean matches(MechanismAnimationGroup group) {
        if (search.isBlank()) return true;
        String needle = search.toLowerCase(Locale.ROOT).trim();
        String haystack = Component.translatable(group.translationKey()).getString() + " "
                + Component.translatable(group.translationKey() + ".search").getString() + " "
                + group.name() + " " + group.configKey();
        return haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    private boolean allMechanismsEnabled() {
        return GmfConfig.CLIENT.mechanismAnimations.values().stream().allMatch(value -> value.get());
    }

    private int enabledMechanismCount() {
        return (int) GmfConfig.CLIENT.mechanismAnimations.values().stream().filter(value -> value.get()).count();
    }

    private void toggleAllMechanisms() {
        boolean next = !allMechanismsEnabled();
        GmfConfig.CLIENT.mechanismAnimations.values().forEach(value -> value.set(next));
        customAndRefresh();
    }

    private void applyPreset(PcProfile preset) {
        GmfConfig.applyProfile(preset);
        GmfConfig.save();
        DistantAnimationController.refreshLoadedKinetics();
    }

    private void reset() {
        GmfConfig.resetAll();
        FlywheelBackendController.apply(FlywheelBackendMode.DEFAULT);
        DistantAnimationController.refreshLoadedKinetics();
    }

    private void customAndSave() {
        GmfConfig.CLIENT.pcProfile.set(PcProfile.CUSTOM);
        GmfConfig.save();
    }

    private void customAndRefresh() {
        customAndSave();
        DistantAnimationController.refreshLoadedKinetics();
    }

    private void switchPage(Page newPage) {
        if (page == newPage) return;
        page = newPage;
        scrollOffset = 0;
        maxScroll = 0;
        rebuildSearchBox();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int index = hitAreas.size() - 1; index >= 0; index--) {
                HitArea area = hitAreas.get(index);
                if (area.contains(mouseX, mouseY)) {
                    area.action.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= contentLeft && mouseX < contentRight && mouseY >= 78 && mouseY < contentViewportBottom()
                && maxScroll > 0) {
            scrollOffset = Math.clamp(scrollOffset - (int) Math.round(scrollY * 28), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    private static boolean contains(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }

    private static <T> T next(T[] values, T current) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == current) return values[(index + 1) % values.length];
        }
        return values[0];
    }

    private static int next(int[] values, int current) {
        for (int value : values) if (value > current) return value;
        return values[0];
    }

    private record HitArea(int x, int y, int width, int height, Runnable action) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
        }
    }

    private enum Page {
        MECHANISMS("gui.create_gmf.nav.mechanisms"),
        PARTICLES("gui.create_gmf.nav.particles"),
        FLYWHEEL("gui.create_gmf.nav.flywheel"),
        SHADERS("gui.create_gmf.nav.shaders"),
        OTHER("gui.create_gmf.nav.other"),
        FEEDBACK("gui.create_gmf.nav.feedback");

        private final String key;

        Page(String key) {
            this.key = key;
        }
    }
}
