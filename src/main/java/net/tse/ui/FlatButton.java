package net.tse.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class FlatButton extends AbstractWidget {

    public interface PressAction { void onPress(FlatButton button); }

    public enum Style {
        DEFAULT,
        ACCENT,
        DANGER,
        GHOST,
        TAB
    }

    private final PressAction onPress;
    private final Style style;
    private boolean selected;
    private float hoverAnim = 0f;

    private FlatButton(int x, int y, int w, int h, Component text, PressAction onPress, Style style, boolean selected) {
        super(x, y, w, h, text);
        this.onPress = onPress;
        this.style = style;
        this.selected = selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public boolean isSelected() {
        return selected;
    }

    public static Builder builder(Component text, PressAction onPress) {
        return new Builder(text, onPress);
    }

    public static class Builder {
        private final Component text;
        private final PressAction onPress;
        private int x, y, w, h;
        private Style style = Style.DEFAULT;
        private boolean selected = false;

        Builder(Component text, PressAction onPress) { this.text = text; this.onPress = onPress; }

        public Builder dimensions(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            return this;
        }

        public Builder style(Style style) {
            this.style = style;
            return this;
        }

        public Builder selected(boolean selected) {
            this.selected = selected;
            return this;
        }

        public FlatButton build() {
            return new FlatButton(x, y, w, h, text, onPress, style, selected);
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
        boolean hovered = isHovered() && active;
        hoverAnim += ((hovered ? 1f : 0f) - hoverAnim) * 0.28f;

        switch (style) {
            case TAB -> {
                if (selected) {
                    MCTheme.fillRounded(context, x, y, w, h, 4, MCTheme.accent(40));
                    MCTheme.drawRoundedOutline(context, x, y, w, h, 4, MCTheme.accent(100));
                    MCTheme.fillRounded(context, x, y + 3, 2, h - 6, 1, MCTheme.accent(255));
                } else if (hoverAnim > 0.02f) {
                    MCTheme.fillRounded(context, x, y, w, h, 4, MCTheme.withAlpha(MCTheme.BUTTON_BG_HOVER, (int) (220 * hoverAnim)));
                }
                int textColor = selected ? MCTheme.TEXT : MCTheme.lerpColor(MCTheme.TEXT_DIM, MCTheme.TEXT, hoverAnim);
                String s = getMessage().getString();
                String trimmed = mc.font.plainSubstrByWidth(s, w - 12);
                MCTheme.drawTextHD(context, mc.font, trimmed, x + 8, y + (h - 8) / 2, textColor, false);
            }
            case DANGER -> {
                int bg = MCTheme.lerpColor(MCTheme.DELETE_BG, MCTheme.DELETE_BG_HOVER, hoverAnim);
                int border = MCTheme.lerpColor(0x44FF5555, 0x99FF6666, hoverAnim);
                MCTheme.fillRoundedCard(context, x, y, w, h, 4, bg, border);
                drawCenteredLabel(context, mc, x, y, w, h, 0xFFFFE5E5);
            }
            case ACCENT -> {
                int bg = MCTheme.lerpColor(MCTheme.accent(38), MCTheme.accent(72), hoverAnim);
                int border = MCTheme.lerpColor(MCTheme.accent(110), MCTheme.accent(195), hoverAnim);
                MCTheme.fillRoundedCard(context, x, y, w, h, 4, bg, border);
                drawCenteredLabel(context, mc, x, y, w, h, MCTheme.TEXT);
            }
            case GHOST -> {
                if (hoverAnim > 0.02f) {
                    MCTheme.fillRounded(context, x, y, w, h, 4, MCTheme.withAlpha(MCTheme.BUTTON_BG_HOVER, (int) (220 * hoverAnim)));
                }
                int color = !active ? MCTheme.TEXT_DIM : MCTheme.lerpColor(MCTheme.TEXT_DIM, MCTheme.accent(255), hoverAnim);
                drawCenteredLabel(context, mc, x, y, w, h, color);
            }
            default -> {
                int bg = MCTheme.lerpColor(MCTheme.BUTTON_BG, MCTheme.BUTTON_BG_HOVER, hoverAnim);
                int border = MCTheme.lerpColor(MCTheme.CARD_BORDER, MCTheme.accent(130), hoverAnim);
                MCTheme.fillRoundedCard(context, x, y, w, h, 4, bg, border);
                int color = !active ? MCTheme.TEXT_DIM : MCTheme.lerpColor(0xFFE0E0E6, MCTheme.TEXT, hoverAnim);
                drawCenteredLabel(context, mc, x, y, w, h, color);
            }
        }
    }

    private void drawCenteredLabel(GuiGraphicsExtractor context, Minecraft mc, int x, int y, int w, int h, int color) {
        String s = getMessage().getString();
        String trimmed = mc.font.plainSubstrByWidth(s, Math.max(4, w - 6));
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