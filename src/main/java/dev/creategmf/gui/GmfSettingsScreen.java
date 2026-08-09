package dev.creategmf.gui;

import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import dev.creategmf.config.CreateParticleMode;
import dev.creategmf.config.DistantAnimationMode;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.MechanismAnimationGroup;
import dev.creategmf.config.PcProfile;
import dev.creategmf.optimization.animations.DistantAnimationController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GmfSettingsScreen extends GmfScreen {
    private static final int[] TARGETS = {30, 45, 60, 75, 90, 120, 144, 165, 240};
    private static final double[] SHADOW_DISTANCES = {0, 16, 24, 32, 48, 64, 96, 128};
    private static final int MAX_ANIMATION_DISTANCE = 256;
    private static final int[] REDUCED_ANIMATION_FPS = {1, 2, 5, 10, 15, 20, 30};
    private static final PcProfile[] GRAPHICS_PRESETS = {
            PcProfile.POTATO, PcProfile.LOW, PcProfile.MEDIUM,
            PcProfile.ABOVE_AVERAGE, PcProfile.HIGH, PcProfile.ULTRA
    };
    private int pendingAnimationRefresh;
    private SettingsList settingsList;
    private EditBox mechanismSearch;
    private String searchQuery = "";

    public GmfSettingsScreen(Screen parent) {
        super(Component.translatable("gui.create_gmf.rendering_settings"), parent);
    }

    @Override
    protected void init() {
        mechanismSearch = new EditBox(font, width / 2 - 190, 40, 380, 20,
                Component.translatable("gui.create_gmf.settings.search"));
        mechanismSearch.setHint(Component.translatable("gui.create_gmf.settings.search_hint"));
        mechanismSearch.setValue(searchQuery);
        mechanismSearch.setResponder(value -> {
            searchQuery = value;
            rebuildList();
        });
        addRenderableWidget(mechanismSearch);

        settingsList = new SettingsList(minecraft, width, Math.max(80, height - 104), 64, 24);
        addRenderableWidget(settingsList);
        rebuildList();
        addBackButton();
    }

    private void rebuildList() {
        if (settingsList == null) {
            return;
        }
        settingsList.clearSettings();
        boolean filtering = !searchQuery.isBlank();

        if (!filtering) {
            populateGeneralSettings(settingsList);
        }

        settingsList.addHeader(Component.translatable("gui.create_gmf.settings.mechanisms"));
        int matches = 0;
        for (MechanismAnimationGroup group : MechanismAnimationGroup.values()) {
            if (filtering && !matchesSearch(group, searchQuery)) {
                continue;
            }
            matches++;
            settingsList.addSetting(() -> mechanismLabel(group), () -> {
                var value = GmfConfig.CLIENT.mechanismAnimations.get(group);
                value.set(!value.get());
                setCustomProfile();
                saveAndRefresh();
            });
        }
        if (matches == 0) {
            settingsList.addInfo(Component.translatable("gui.create_gmf.settings.search_empty", searchQuery));
        }

        if (!filtering) {
            settingsList.addHeader(Component.translatable("gui.create_gmf.settings.reset_section"));
            settingsList.addSetting(() -> Component.translatable("gui.create_gmf.reset_all"), () -> {
                GmfConfig.resetAll();
                DistantAnimationController.refreshLoadedKinetics();
                minecraft.setScreen(new GmfSettingsScreen(parent));
            });
        }
    }

    private void populateGeneralSettings(SettingsList list) {
        list.addHeader(Component.translatable("gui.create_gmf.settings.presets"));
        list.addInfo(Component.translatable("gui.create_gmf.settings.presets_hint"));
        list.addSetting(this::profileLabel, () -> {
            GmfConfig.applyProfile(nextProfile(GmfConfig.CLIENT.pcProfile.get()));
            saveAndRefresh();
            minecraft.setScreen(new GmfSettingsScreen(parent));
        });

        list.addHeader(Component.translatable("gui.create_gmf.settings.general"));
        list.addInfo(Component.translatable("gui.create_gmf.settings.visual_only"));
        list.addSetting(this::enabledLabel, () -> {
            GmfConfig.CLIENT.enabled.set(!GmfConfig.CLIENT.enabled.get());
            saveAndRefresh();
        });
        list.addSetting(this::targetLabel, () -> {
            GmfConfig.CLIENT.targetFps.set(nextTarget(GmfConfig.CLIENT.targetFps.get()));
            setCustomProfile();
            GmfConfig.save();
        });
        list.addSetting(this::overlayLabel, () -> {
            GmfConfig.CLIENT.showOverlay.set(!GmfConfig.CLIENT.showOverlay.get());
            GmfConfig.save();
        });
        list.addSetting(this::particleModeLabel, () -> {
            GmfConfig.CLIENT.createParticleMode.set(nextEnum(
                    CreateParticleMode.values(), GmfConfig.CLIENT.createParticleMode.get()));
            setCustomProfile();
            GmfConfig.save();
        });
        list.addSetting(this::animationModeLabel, () -> {
            GmfConfig.CLIENT.distantAnimationMode.set(nextEnum(
                    DistantAnimationMode.values(), GmfConfig.CLIENT.distantAnimationMode.get()));
            setCustomProfile();
            saveAndRefresh();
        });
        list.addAnimationDistanceSlider(GmfConfig.CLIENT.distantAnimationDistance.get().intValue(),
                this::setAnimationDistance);
        list.addSetting(this::reducedAnimationFpsLabel, () -> {
            GmfConfig.CLIENT.reducedAnimationFps.set(
                    nextTarget(REDUCED_ANIMATION_FPS, GmfConfig.CLIENT.reducedAnimationFps.get()));
            setCustomProfile();
            GmfConfig.save();
        });
        list.addInfo(Component.translatable("gui.create_gmf.settings.zero_fluid_note"));
        list.addSetting(this::shadowToggleLabel, () -> {
            GmfConfig.CLIENT.beltItemShadowOptimization.set(!GmfConfig.CLIENT.beltItemShadowOptimization.get());
            GmfConfig.save();
        });
        list.addSetting(this::shadowDistanceLabel, () -> {
            GmfConfig.CLIENT.beltItemShadowDistance.set(
                    nextDistance(SHADOW_DISTANCES, GmfConfig.CLIENT.beltItemShadowDistance.get()));
            setCustomProfile();
            GmfConfig.save();
        });

    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderPanel(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void tick() {
        if (pendingAnimationRefresh > 0 && --pendingAnimationRefresh == 0) {
            saveAndRefresh();
        }
    }

    @Override
    public void removed() {
        if (pendingAnimationRefresh > 0) {
            pendingAnimationRefresh = 0;
            saveAndRefresh();
        }
        super.removed();
    }

    private Component enabledLabel() {
        return setting("config.create_gmf.enable", booleanValue(GmfConfig.CLIENT.enabled.get()));
    }

    private Component profileLabel() {
        return setting("config.create_gmf.graphics_preset",
                Component.translatable(GmfConfig.CLIENT.pcProfile.get().translationKey()));
    }

    private Component targetLabel() {
        return setting("config.create_gmf.target_fps",
                Component.literal(Integer.toString(GmfConfig.CLIENT.targetFps.get())));
    }

    private Component overlayLabel() {
        return setting("config.create_gmf.show_statistics", booleanValue(GmfConfig.CLIENT.showOverlay.get()));
    }

    private Component shadowToggleLabel() {
        return setting("config.create_gmf.belt_item_shadows",
                booleanValue(GmfConfig.CLIENT.beltItemShadowOptimization.get()));
    }

    private Component shadowDistanceLabel() {
        return setting("config.create_gmf.belt_item_shadow_distance_blocks",
                Component.literal(Integer.toString(GmfConfig.CLIENT.beltItemShadowDistance.get().intValue())));
    }

    private Component particleModeLabel() {
        return setting("config.create_gmf.create_particles",
                Component.translatable(GmfConfig.CLIENT.createParticleMode.get().translationKey()));
    }

    private Component animationModeLabel() {
        return setting("config.create_gmf.distant_animations",
                Component.translatable(GmfConfig.CLIENT.distantAnimationMode.get().translationKey()));
    }

    private Component reducedAnimationFpsLabel() {
        return setting("config.create_gmf.reduced_animation_fps",
                Component.translatable("unit.create_gmf.fps", GmfConfig.CLIENT.reducedAnimationFps.get()));
    }

    private void setAnimationDistance(int blocks) {
        GmfConfig.CLIENT.distantAnimationDistance.set((double) blocks);
        setCustomProfile();
        pendingAnimationRefresh = 6;
    }

    private Component mechanismLabel(MechanismAnimationGroup group) {
        return setting(group.translationKey(),
                booleanValue(GmfConfig.CLIENT.mechanismAnimations.get(group).get()));
    }

    private boolean matchesSearch(MechanismAnimationGroup group, String query) {
        String needle = query.toLowerCase(Locale.ROOT).trim();
        String searchable = Component.translatable(group.translationKey()).getString() + " "
                + Component.translatable(group.translationKey() + ".search").getString() + " "
                + group.name() + " " + group.configKey();
        return searchable.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void saveAndRefresh() {
        GmfConfig.save();
        DistantAnimationController.refreshLoadedKinetics();
    }

    private static void setCustomProfile() {
        GmfConfig.CLIENT.pcProfile.set(PcProfile.CUSTOM);
    }

    private static PcProfile nextProfile(PcProfile current) {
        for (int index = 0; index < GRAPHICS_PRESETS.length; index++) {
            if (GRAPHICS_PRESETS[index] == current) {
                return GRAPHICS_PRESETS[(index + 1) % GRAPHICS_PRESETS.length];
            }
        }
        return GRAPHICS_PRESETS[0];
    }

    private static Component setting(String key, Component value) {
        return Component.translatable("gui.create_gmf.setting_value", Component.translatable(key), value);
    }

    private static Component booleanValue(boolean value) {
        return Component.translatable(value ? "enum.create_gmf.on" : "enum.create_gmf.off");
    }

    private static int nextTarget(int current) {
        return nextTarget(TARGETS, current);
    }

    private static int nextTarget(int[] values, int current) {
        for (int target : values) {
            if (target > current) return target;
        }
        return values[0];
    }

    private static double nextDistance(double[] values, double current) {
        for (double distance : values) {
            if (distance > current) return distance;
        }
        return values[0];
    }

    private static <T> T nextEnum(T[] values, T current) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == current) return values[(index + 1) % values.length];
        }
        return values[0];
    }

    private static final class SettingsList extends ContainerObjectSelectionList<SettingsEntry> {
        private SettingsList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
            centerListVertically = false;
        }

        private void addSetting(Supplier<Component> message, Runnable action) {
            addEntry(new ButtonEntry(minecraft, message, action));
        }

        private void addHeader(Component title) {
            addEntry(new HeaderEntry(minecraft, title));
        }

        private void addInfo(Component text) {
            addEntry(new InfoEntry(minecraft, text));
        }

        private void addAnimationDistanceSlider(int initialValue, IntConsumer onChange) {
            addEntry(new SliderEntry(initialValue, onChange));
        }

        private void clearSettings() {
            clearEntries();
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, getWidth() - 28);
        }

        @Override
        protected int getScrollbarPosition() {
            return getRowRight() + 4;
        }
    }

    private abstract static class SettingsEntry extends ContainerObjectSelectionList.Entry<SettingsEntry> {
    }

    private static final class ButtonEntry extends SettingsEntry {
        private final Supplier<Component> message;
        private final Button button;

        private ButtonEntry(Minecraft minecraft, Supplier<Component> message, Runnable action) {
            this.message = message;
            button = Button.builder(message.get(), ignored -> {
                action.run();
                buttonMessageRefresh();
            }).bounds(0, 0, 360, 20).build();
        }

        private void buttonMessageRefresh() {
            button.setMessage(message.get());
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            button.setX(left + 4);
            button.setY(top);
            button.setWidth(width - 8);
            button.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(button);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(button);
        }
    }

    private static final class HeaderEntry extends SettingsEntry {
        private final Minecraft minecraft;
        private final Component title;

        private HeaderEntry(Minecraft minecraft, Component title) {
            this.minecraft = minecraft;
            this.title = title;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawCenteredString(minecraft.font, title, left + width / 2, top + 6, ACCENT_COLOR);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    private static final class SliderEntry extends SettingsEntry {
        private final AnimationDistanceSlider slider;

        private SliderEntry(int initialValue, IntConsumer onChange) {
            slider = new AnimationDistanceSlider(initialValue, onChange);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            slider.setX(left + 4);
            slider.setY(top);
            slider.setWidth(width - 8);
            slider.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(slider);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(slider);
        }
    }

    private static final class AnimationDistanceSlider extends AbstractSliderButton {
        private final IntConsumer onChange;

        private AnimationDistanceSlider(int initialValue, IntConsumer onChange) {
            super(0, 0, 360, 20, Component.empty(),
                    Math.clamp(initialValue, 0, MAX_ANIMATION_DISTANCE) / (double) MAX_ANIMATION_DISTANCE);
            this.onChange = onChange;
            updateMessage();
        }

        private int blocks() {
            return (int) Math.round(value * MAX_ANIMATION_DISTANCE);
        }

        @Override
        protected void updateMessage() {
            Component valueLabel = blocks() == 0
                    ? Component.translatable("enum.create_gmf.disabled")
                    : Component.translatable("unit.create_gmf.blocks", blocks());
            setMessage(Component.translatable("config.create_gmf.animation_distance_slider", valueLabel));
        }

        @Override
        protected void applyValue() {
            onChange.accept(blocks());
        }
    }

    private static final class InfoEntry extends SettingsEntry {
        private final Minecraft minecraft;
        private final Component text;

        private InfoEntry(Minecraft minecraft, Component text) {
            this.minecraft = minecraft;
            this.text = text;
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.drawCenteredString(minecraft.font, text, left + width / 2, top + 6, 0xFFB8C1CC);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }
}
