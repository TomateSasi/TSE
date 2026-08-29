package net.tse.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tse.ui.FlatButton;
import java.util.function.IntConsumer;


public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final IntConsumer onDone;
    private int a, r, g, b;
    private final int originalArgb;

    public ColorPickerScreen(Screen parent, int argb, IntConsumer onDone) {
        super(Component.literal("Color"));
        this.parent = parent;
        this.onDone = onDone;
        this.originalArgb = argb;
        this.a = (argb >>> 24) & 0xFF;
        this.r = (argb >>> 16) & 0xFF;
        this.g = (argb >>> 8) & 0xFF;
        this.b = argb & 0xFF;
    }

    private int packed() { return (a << 24) | (r << 16) | (g << 8) | b; }

    @Override
    protected void init() {
        int cx = width / 2;
        int top = height / 2 - 90;
        int sliderW = 240;

        addRenderableWidget(new PreciseSlider(cx - sliderW / 2, top + 40, sliderW, r, 0, 255, 1,
                v -> String.format("%.0f", v), v -> r = (int) v));
        addRenderableWidget(new PreciseSlider(cx - sliderW / 2, top + 62, sliderW, g, 0, 255, 1,
                v -> String.format("%.0f", v), v -> g = (int) v));
        addRenderableWidget(new PreciseSlider(cx - sliderW / 2, top + 84, sliderW, b, 0, 255, 1,
                v -> String.format("%.0f", v), v -> b = (int) v));
        addRenderableWidget(new PreciseSlider(cx - sliderW / 2, top + 106, sliderW, a, 0, 255, 1,
                v -> String.format("%.0f", v), v -> a = (int) v));

        addRenderableWidget(FlatButton.builder(Component.literal("Done"), btn -> {
                    onDone.accept(packed());
                    Minecraft.getInstance().setScreen(parent);
                })
                .dimensions(cx - 105, top + 140, 100, 20).build());
        addRenderableWidget(FlatButton.builder(Component.literal("Cancel"), btn ->
                        Minecraft.getInstance().setScreen(parent))
                .dimensions(cx + 5, top + 140, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parent != null) parent.extractRenderState(context, -1, -1, delta);
        int cx = width / 2;
        int top = height / 2 - 90;
        int panelW = 280, panelH = 200;
        context.fill(cx - panelW / 2, top - 20, cx + panelW / 2, top - 20 + panelH, MCTheme.PANEL_BG);
        context.outline(cx - panelW / 2, top - 20, panelW, panelH, MCTheme.CARD_BORDER);

        context.centeredText(font, "Edit Color", cx, top - 10, MCTheme.TEXT);

        int swW = 40, swH = 16;
        context.fill(cx - swW / 2, top + 14, cx + swW / 2, top + 14 + swH, 0xFF1A1A1A);
        context.fill(cx - swW / 2 + 1, top + 15, cx + swW / 2 - 1, top + 14 + swH - 1, packed());

        MCTheme.drawTextHD(context, font, "R", cx - 240 / 2 - 12, top + 41, 0xFFFF6666, false);
        MCTheme.drawTextHD(context, font, "G", cx - 240 / 2 - 12, top + 63, 0xFF66FF66, false);
        MCTheme.drawTextHD(context, font, "B", cx - 240 / 2 - 12, top + 85, 0xFF6666FF, false);
        MCTheme.drawTextHD(context, font, "A", cx - 240 / 2 - 12, top + 107, MCTheme.TEXT_DIM, false);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }
}