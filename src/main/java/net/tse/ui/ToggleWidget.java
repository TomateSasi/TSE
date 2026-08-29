package net.tse.ui;

import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;


public class ToggleWidget extends AbstractWidget {

    public static final int W = 22, H = 12;

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
        int track = value ? MCTheme.accent() : MCTheme.FIELD_BG;
        if (isHovered()) track = MCTheme.withAlpha(track, 255);
        MCTheme.fillRounded(context, x, y, w, h, 6, track);

        int knobR = h / 2 - 2;
        int knobCx = value ? (x + w - h / 2) : (x + h / 2);
        int knobCy = y + h / 2;
        MCTheme.fillRounded(context, knobCx - knobR, knobCy - knobR, knobR * 2, knobR * 2, knobR, 0xFFFFFFFF);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE,
                Component.literal(value ? "On" : "Off"));
    }
}