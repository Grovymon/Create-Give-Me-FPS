package dev.creategmf.legacy;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Compatibility builder backed by the real pre-1.19.3 Button constructor. */
public final class GmfButtons {
    private GmfButtons() {}
    public static Builder builder(Component label, Button.OnPress action) { return new Builder(label, action); }
    public static final class Builder {
        private final Component label;
        private final Button.OnPress action;
        private int x, y, width = 150, height = 20;
        private Builder(Component label, Button.OnPress action) { this.label = label; this.action = action; }
        public Builder bounds(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = width; this.height = height; return this;
        }
        public Button build() { return new Button(x, y, width, height, label, action); }
    }
}
