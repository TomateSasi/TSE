package net.tse.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;

public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final IntConsumer onDone;
    private int a, r, g, b;

    public ColorPickerScreen(Screen parent, int argb, IntConsumer onDone) {
        super(Component.literal("Color"));
        this.parent = parent;
        this.onDone = onDone;
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
        int sliderW = 230;

        addRenderableWidget(new PreciseSlider(cx - sliderW / 2 + 8, top + 44, sliderW, r, 0, 255, 1,
                v -> String.format("%.0f", v), v -> r = (int) v));
        addRenderableWidget(new PreciseSlider(cx - sliderW / 2 + 8, top + 68, sliderW, g, 0, 255, 1,
                v -> String.format("%.0f", v), v -> g = (int) v));
        addRenderableWidget(new PreciseSlider(cx - sliderW / 2 + 8, top + 92, sliderW, b, 0, 255, 1,
                v -> String.format("%.0f", v), v -> b = (int) v));
        addRenderableWidget(new PreciseSlider(cx - sliderW / 2 + 8, top + 116, sliderW, a, 0, 255, 1,
                v -> String.format("%.0f", v), v -> a = (int) v));

        addRenderableWidget(FlatButton.builder(Component.literal("Save Color"), btn -> {
                    onDone.accept(packed());
                    Minecraft.getInstance().gui.setScreen(parent);
                })
                .dimensions(cx - 108, top + 146, 102, 20)
                .style(FlatButton.Style.ACCENT)
                .build());
        addRenderableWidget(FlatButton.builder(Component.literal("Cancel"), btn ->
                        Minecraft.getInstance().gui.setScreen(parent))
                .dimensions(cx + 6, top + 146, 102, 20)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parent != null) parent.extractRenderState(context, -1, -1, delta);
        context.fill(0, 0, width, height, 0x77000000);

        int cx = width / 2;
        int top = height / 2 - 90;
        int panelW = 290, panelH = 198;
        MCTheme.fillRoundedCard(context, cx - panelW / 2, top - 16, panelW, panelH, 10, MCTheme.WINDOW_BG, MCTheme.CARD_BORDER);

        MCTheme.drawTextHD(context, font, "Color Picker", cx - panelW / 2 + 14, top - 4, MCTheme.TEXT, false);
        String hex = String.format("#%02X%02X%02X%02X", a, r, g, b);
        MCTheme.drawTextHD(context, font, hex, cx + panelW / 2 - font.width(hex) - 14, top - 4, MCTheme.TEXT_DIM, false);
        context.fill(cx - panelW / 2 + 10, top + 9, cx + panelW / 2 - 10, top + 10, MCTheme.BORDER);

        int swW = panelW - 28, swH = 16;
        MCTheme.fillRoundedCard(context, cx - swW / 2, top + 16, swW, swH, 4, packed(), MCTheme.CARD_BORDER);

        int labelX = cx - 230 / 2 - 8;
        MCTheme.drawTextHD(context, font, "R", labelX, top + 48, 0xFFFF6666, false);
        MCTheme.drawTextHD(context, font, "G", labelX, top + 72, 0xFF66FF66, false);
        MCTheme.drawTextHD(context, font, "B", labelX, top + 96, 0xFF6688FF, false);
        MCTheme.drawTextHD(context, font, "A", labelX, top + 120, MCTheme.TEXT_DIM, false);

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}