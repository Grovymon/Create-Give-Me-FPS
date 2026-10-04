package dev.creategmf.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import dev.creategmf.config.GmfConfig;
import dev.creategmf.config.PcProfile;
import dev.creategmf.integration.CreateVisualClassifier;
import dev.creategmf.optimization.animations.DistantAnimationController;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/** Mechanisms > installed addon > observed compatible registry types. Discovery is not performed per frame. */
public final class GmfAddonScreen extends GmfScreen {
    private final String namespace;
    private List<CreateVisualClassifier.Owner> owners = List.of();
    private int scroll;
    public GmfAddonScreen(Screen parent, String namespace) {
        super(Component.translatable("gui.create_gmf.addons"), parent);
        this.namespace = namespace;
    }
    @Override protected void init() {
        owners = CreateVisualClassifier.observedOwners().stream()
                .filter(o -> o.supported() && !"create".equals(o.namespace()) && ModList.get().isLoaded(o.namespace()))
                .sorted(Comparator.comparing(CreateVisualClassifier.Owner::registryId)).toList();
        rebuild();
    }
    private void rebuild() {
        clearWidgets();
        int x = width / 2 - Math.min(180, (width - 40) / 2);
        int w = Math.max(1, Math.min(360, width - 40));
        int firstY = 44 + font.split(Component.translatable("gui.create_gmf.addon.discovery"), w).size() * font.lineHeight;
        int y = firstY;
        List<String> ids = namespace == null ? owners.stream().map(CreateVisualClassifier.Owner::namespace).distinct().toList()
                : owners.stream().filter(o -> namespace.equals(o.namespace())).map(CreateVisualClassifier.Owner::registryId).toList();
        int capacity = Math.max(0, (height - 36 - firstY) / 24);
        scroll = Math.clamp(scroll, 0, Math.max(0, ids.size() - capacity));
        for (int index = scroll; index < Math.min(ids.size(), scroll + capacity); index++) {
            String id = ids.get(index);
            Component label = namespace == null
                    ? Component.literal(ModList.get().getModContainerById(id).orElseThrow().getModInfo().getDisplayName())
                    : Component.translatable("gui.create_gmf.addon_mechanism", id,
                            Component.translatable(GmfConfig.CLIENT.disabledAddonMechanisms.get().contains(id)
                                    ? "gui.create_gmf.addon.disabled" : "gui.create_gmf.addon.enabled"));
            addRenderableWidget(Button.builder(label, button -> {
                if (namespace == null) minecraft.setScreen(new GmfAddonScreen(this, id));
                else {
                    List<String> disabled = new ArrayList<>(GmfConfig.CLIENT.disabledAddonMechanisms.get());
                    if (!disabled.remove(id)) disabled.add(id);
                    GmfConfig.CLIENT.disabledAddonMechanisms.set(List.copyOf(disabled));
                    GmfConfig.CLIENT.pcProfile.set(PcProfile.CUSTOM);
                    GmfConfig.save();
                    DistantAnimationController.refreshLoadedKinetics();
                    rebuild();
                }
            }).bounds(x, y, w, 20).build());
            y += 24;
        }
        addBackButton();
    }
    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) {
        scroll += dy > 0 ? -1 : 1;
        rebuild();
        return true;
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partialTick) {
        renderPanel(graphics);
        graphics.drawWordWrap(font, Component.translatable("gui.create_gmf.addon.discovery"),
                width / 2 - Math.min(180, (width - 40) / 2), 40, Math.min(360, width - 40), 0xFFB8C1CC);
        super.render(graphics, x, y, partialTick);
    }
}
