package net.tse.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.function.IntConsumer;

public class ColorPickerScreen extends Screen {

    private final Screen parent;
    private final IntConsumer onDone;

    private float hue;
    private float sat;
    private float val;
    private int alpha;

    private int panelX, panelY;
    private static final int PANEL_W = 310, PANEL_H = 220;

    private int svX, svY;
    private static final int SV_W = 160, SV_H = 116;

    private int hueX, hueY;
    private static final int BAR_W = 14, BAR_H = 116;

    private int alphaX, alphaY;

    private int dragMode = 0;
    private FlatTextField hexField;
    private boolean updatingHex = false;

    private static final int[] SWATCHES = {
            0xFFFFFFFF, 0xFFFF4444, 0xFFFF8833, 0xFFFFDD44,
            0xFF44DD66, 0xFF33DDDD, 0xFF4488FF, 0xFFAA55FF,
            0xFFFF66B2, 0xFF94949E
    };

    public ColorPickerScreen(Screen parent, int argb, IntConsumer onDone) {
        super(Component.literal("Color Picker"));
        this.parent = parent;
        this.onDone = onDone;

        this.alpha = (argb >>> 24) & 0xFF;
        if (this.alpha == 0) this.alpha = 255;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        this.hue = hsb[0];
        this.sat = hsb[1];
        this.val = hsb[2];
    }

    private int packed() {
        int rgb = Color.HSBtoRGB(hue, sat, val) & 0x00FFFFFF;
        return ((alpha & 0xFF) << 24) | rgb;
    }

    private void syncHexField() {
        if (hexField == null) return;
        updatingHex = true;
        hexField.setValue(String.format("#%08X", packed()));
        updatingHex = false;
    }

    @Override
    protected void init() {
        clearWidgets();
        panelX = (width - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        svX = panelX + 14;
        svY = panelY + 32;

        hueX = svX + SV_W + 10;
        hueY = svY;

        alphaX = hueX + BAR_W + 8;
        alphaY = svY;

        int rightColX = alphaX + BAR_W + 12;
        int rightColW = (panelX + PANEL_W - 14) - rightColX;

        hexField = new FlatTextField(font, rightColX, svY + 34, rightColW, 18, Component.literal("Hex"));
        hexField.setMaxLength(9);
        hexField.setValue(String.format("#%08X", packed()));
        hexField.setResponder(s -> {
            if (updatingHex) return;
            String clean = s.startsWith("#") ? s.substring(1) : s;
            if (clean.length() == 6 || clean.length() == 8) {
                try {
                    long parsed = Long.parseLong(clean, 16);
                    if (clean.length() == 6) {
                        alpha = 255;
                    } else {
                        alpha = (int) ((parsed >>> 24) & 0xFF);
                    }
                    int r = (int) ((parsed >>> 16) & 0xFF);
                    int g = (int) ((parsed >>> 8) & 0xFF);
                    int b = (int) (parsed & 0xFF);
                    float[] hsb = Color.RGBtoHSB(r, g, b, null);
                    hue = hsb[0];
                    sat = hsb[1];
                    val = hsb[2];
                } catch (NumberFormatException ignored) {}
            }
        });
        addRenderableWidget(hexField);

        addRenderableWidget(FlatButton.builder(Component.literal("Apply Color"), btn -> {
                    onDone.accept(packed());
                    MCTheme.showToast("Color updated");
                    Minecraft.getInstance().gui.setScreen(parent);
                })
                .dimensions(panelX + 14, panelY + PANEL_H - 28, 136, 20)
                .style(FlatButton.Style.ACCENT)
                .build());

        addRenderableWidget(FlatButton.builder(Component.literal("Cancel"), btn ->
                        Minecraft.getInstance().gui.setScreen(parent))
                .dimensions(panelX + PANEL_W - 150, panelY + PANEL_H - 28, 136, 20)
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parent != null) parent.extractRenderState(context, -1, -1, delta);
        context.fill(0, 0, width, height, 0x80000000);

        MCTheme.fillRoundedCard(context, panelX, panelY, PANEL_W, PANEL_H, 10, MCTheme.WINDOW_BG, MCTheme.CARD_BORDER);
        MCTheme.drawTextHD(context, font, "Visual Color Picker", panelX + 14, panelY + 11, MCTheme.TEXT, false);
        context.fill(panelX + 12, panelY + 24, panelX + PANEL_W - 12, panelY + 25, MCTheme.BORDER);

        int step = 4;
        for (int py = 0; py < SV_H; py += step) {
            float v = 1f - (py / (float) SV_H);
            int hSlice = Math.min(step, SV_H - py);
            for (int px = 0; px < SV_W; px += step) {
                float s = px / (float) SV_W;
                int wSlice = Math.min(step, SV_W - px);
                int col = 0xFF000000 | (Color.HSBtoRGB(hue, s, v) & 0x00FFFFFF);
                context.fill(svX + px, svY + py, svX + px + wSlice, svY + py + hSlice, col);
            }
        }
        context.outline(svX - 1, svY - 1, SV_W + 2, SV_H + 2, MCTheme.CARD_BORDER);

        int curX = svX + Math.round(sat * SV_W);
        int curY = svY + Math.round((1f - val) * SV_H);
        MCTheme.fillRoundedCard(context, curX - 4, curY - 4, 8, 8, 4, packed() | 0xFF000000, 0xFFFFFFFF);

        for (int py = 0; py < BAR_H; py += 2) {
            float hVal = py / (float) BAR_H;
            int col = 0xFF000000 | (Color.HSBtoRGB(hVal, 1f, 1f) & 0x00FFFFFF);
            context.fill(hueX, hueY + py, hueX + BAR_W, hueY + Math.min(BAR_H, py + 2), col);
        }
        context.outline(hueX - 1, hueY - 1, BAR_W + 2, BAR_H + 2, MCTheme.CARD_BORDER);
        int hueKnobY = hueY + Math.round(hue * (BAR_H - 2));
        context.fill(hueX - 2, hueKnobY, hueX + BAR_W + 2, hueKnobY + 2, 0xFFFFFFFF);

        int pureRgb = Color.HSBtoRGB(hue, sat, val) & 0x00FFFFFF;
        for (int py = 0; py < BAR_H; py += 2) {
            int aVal = Math.round((1f - (py / (float) BAR_H)) * 255f);
            int col = MCTheme.lerpColor(0xFF18181C, 0xFF000000 | pureRgb, aVal / 255f);
            context.fill(alphaX, alphaY + py, alphaX + BAR_W, alphaY + Math.min(BAR_H, py + 2), col);
        }
        context.outline(alphaX - 1, alphaY - 1, BAR_W + 2, BAR_H + 2, MCTheme.CARD_BORDER);
        int alphaKnobY = alphaY + Math.round((1f - (alpha / 255f)) * (BAR_H - 2));
        context.fill(alphaX - 2, alphaKnobY, alphaX + BAR_W + 2, alphaKnobY + 2, 0xFFFFFFFF);

        int rightColX = alphaX + BAR_W + 12;
        int rightColW = (panelX + PANEL_W - 14) - rightColX;
        MCTheme.fillRoundedCard(context, rightColX, svY, rightColW, 22, 4, packed(), MCTheme.CARD_BORDER);
        MCTheme.drawTextHD(context, font, "HEX", rightColX, svY + 24, MCTheme.TEXT_DIM, false);
        MCTheme.drawTextHD(context, font, "Alpha: " + Math.round((alpha / 255f) * 100f) + "%", rightColX, svY + 58, MCTheme.TEXT_DIM, false);

        int swatchStartY = svY + SV_H + 10;
        MCTheme.drawTextHD(context, font, "Presets:", svX, swatchStartY + 4, MCTheme.TEXT_DIM, false);
        int swX = svX + 48;
        for (int i = 0; i < SWATCHES.length; i++) {
            int c = SWATCHES[i];
            int bx = swX + i * 20;
            MCTheme.fillRoundedCard(context, bx, swatchStartY, 16, 16, 4, c, MCTheme.CARD_BORDER);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
        MCTheme.renderToasts(context, font, width, height);
    }

    private void updateFromMouse(double mx, double my) {
        if (dragMode == 1) {
            sat = (float) Math.max(0.0, Math.min(1.0, (mx - svX) / SV_W));
            val = (float) Math.max(0.0, Math.min(1.0, 1.0 - (my - svY) / SV_H));
            syncHexField();
        } else if (dragMode == 2) {
            hue = (float) Math.max(0.0, Math.min(1.0, (my - hueY) / BAR_H));
            syncHexField();
        } else if (dragMode == 3) {
            float aNorm = (float) Math.max(0.0, Math.min(1.0, 1.0 - (my - alphaY) / BAR_H));
            alpha = Math.round(aNorm * 255f);
            syncHexField();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mx = click.x(), my = click.y();
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (mx >= svX && mx <= svX + SV_W && my >= svY && my <= svY + SV_H) {
                dragMode = 1;
                updateFromMouse(mx, my);
                return true;
            }
            if (mx >= hueX && mx <= hueX + BAR_W && my >= hueY && my <= hueY + BAR_H) {
                dragMode = 2;
                updateFromMouse(mx, my);
                return true;
            }
            if (mx >= alphaX && mx <= alphaX + BAR_W && my >= alphaY && my <= alphaY + BAR_H) {
                dragMode = 3;
                updateFromMouse(mx, my);
                return true;
            }

            int swatchStartY = svY + SV_H + 10;
            int swX = svX + 48;
            for (int i = 0; i < SWATCHES.length; i++) {
                int bx = swX + i * 20;
                if (mx >= bx && mx <= bx + 16 && my >= swatchStartY && my <= swatchStartY + 16) {
                    int c = SWATCHES[i];
                    float[] hsb = Color.RGBtoHSB((c >>> 16) & 0xFF, (c >>> 8) & 0xFF, c & 0xFF, null);
                    hue = hsb[0];
                    sat = hsb[1];
                    val = hsb[2];
                    syncHexField();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (dragMode != 0) {
            updateFromMouse(click.x(), click.y());
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragMode != 0) {
            dragMode = 0;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}