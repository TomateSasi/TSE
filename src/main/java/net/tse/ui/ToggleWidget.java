package net.tse.ui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class ToggleWidget extends AbstractWidget {

    public static final int W = 24, H = 12;

    private boolean value;
    private float animProgress;
    private float hoverProgress;
    private final Consumer<Boolean> onChange;

    public ToggleWidget(int x, int y, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, W, H, Component.empty());
        this.value = initial;
        this.animProgress = initial ? 1f : 0f;
        this.onChange = onChange;
    }

    public boolean get() { return value; }
    public void set(boolean v) { this.value = v; }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        value = !value;
        if (onChange != null) onChange.accept(value);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();

        animProgress += ((value ? 1f : 0f) - animProgress) * 0.28f;
        hoverProgress += ((isHovered() ? 1f : 0f) - hoverProgress) * 0.28f;

        int offTrack = MCTheme.lerpColor(MCTheme.FIELD_BG, MCTheme.BUTTON_BG_HOVER, hoverProgress);
        int onTrack = MCTheme.accent(Math.round(205 + 40 * hoverProgress));
        int track = MCTheme.lerpColor(offTrack, onTrack, animProgress);

        int offBorder = MCTheme.lerpColor(MCTheme.BORDER, MCTheme.CARD_BORDER, hoverProgress);
        int border = MCTheme.lerpColor(offBorder, MCTheme.accent(255), animProgress);

        MCTheme.fillRoundedCard(context, x, y, w, h, h / 2, track, border);

        int knobSize = h - 4;
        int minX = x + 2;
        int maxX = x + w - knobSize - 2;
        int knobX = Math.round(minX + (maxX - minX) * animProgress);
        int knobY = y + 2;
        int knobColor = MCTheme.lerpColor(0xFFCCCCD4, 0xFFFFFFFF, animProgress);
        MCTheme.fillRounded(context, knobX, knobY, knobSize, knobSize, knobSize / 2, knobColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                Component.literal(value ? "On" : "Off"));
    }
}