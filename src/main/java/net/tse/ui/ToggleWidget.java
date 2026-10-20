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
    private final Consumer<Boolean> onChange;

    public ToggleWidget(int x, int y, boolean initial, Consumer<Boolean> onChange) {
        super(x, y, W, H, Component.empty());
        this.value = initial;
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
        int track = value ? MCTheme.accent(isHovered() ? 245 : 210) : (isHovered() ? MCTheme.BUTTON_BG_HOVER : MCTheme.FIELD_BG);
        int border = value ? MCTheme.accent(255) : (isHovered() ? MCTheme.CARD_BORDER : MCTheme.BORDER);
        MCTheme.fillRoundedCard(context, x, y, w, h, h / 2, track, border);

        int knobSize = h - 4;
        int knobX = value ? (x + w - knobSize - 2) : (x + 2);
        int knobY = y + 2;
        MCTheme.fillRounded(context, knobX, knobY, knobSize, knobSize, knobSize / 2, value ? 0xFFFFFFFF : 0xFFCCCCD4);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                Component.literal(value ? "On" : "Off"));
    }
}