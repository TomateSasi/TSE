package net.tse.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;


public class FlatButton extends AbstractWidget {

    public interface PressAction { void onPress(FlatButton button); }

    private final PressAction onPress;

    private FlatButton(int x, int y, int w, int h, Component text, PressAction onPress) {
        super(x, y, w, h, text);
        this.onPress = onPress;
    }

    public static Builder builder(Component text, PressAction onPress) {
        return new Builder(text, onPress);
    }

    public static class Builder {
        private final Component text;
        private final PressAction onPress;
        private int x, y, w, h;

        Builder(Component text, PressAction onPress) { this.text = text; this.onPress = onPress; }

        public Builder dimensions(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            return this;
        }

        public FlatButton build() {
            return new FlatButton(x, y, w, h, text, onPress);
        }
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        if (active && visible && onPress != null) onPress.onPress(this);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        var mc = Minecraft.getInstance();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hovered = isHovered();
        int color = !active ? MCTheme.TEXT_DIM : (hovered ? MCTheme.accent(255) : MCTheme.TEXT);
        String s = getMessage().getString();
        String trimmed = mc.font.plainSubstrByWidth(s, w);
        int textW = mc.font.width(trimmed);
        int tx = x + Math.max(0, (w - textW) / 2);
        int ty = y + (h - 8) / 2;
        MCTheme.drawTextHD(context, mc.font, trimmed, tx, ty, color, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, getMessage());
    }
}