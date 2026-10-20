package net.tse.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public final class MCTheme {

    private MCTheme() {}

    public static final Identifier FONT = Identifier.fromNamespaceAndPath("minecraft", "default");
    private static final Style FONT_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(FONT));

    public static Component styled(String s) {
        return Component.literal(s).setStyle(FONT_STYLE);
    }

    public static void drawTextHD(GuiGraphicsExtractor context, net.minecraft.client.gui.Font textRenderer,
                                  String s, int x, int y, int color, boolean shadow) {
        context.text(textRenderer, styled(s), x, y, color, shadow);
    }

    public static final String[] THEME_NAMES = {
            "Default", "Blue", "Green", "Purple", "Orange", "Pink", "Cyan", "Gold"
    };

    private static final int[] THEME_ACCENTS = {
            argb(255, 220, 40, 40),
            argb(255, 50, 130, 255),
            argb(255, 40, 185, 80),
            argb(255, 155, 60, 255),
            argb(255, 215, 96, 20),
            argb(255, 230, 80, 160),
            argb(255, 30, 200, 220),
            argb(255, 220, 175, 0),
    };

    private static int currentAccent = THEME_ACCENTS[0];

    public static void applyTheme(String name) {
        for (int i = 0; i < THEME_NAMES.length; i++) {
            if (THEME_NAMES[i].equalsIgnoreCase(name)) {
                currentAccent = THEME_ACCENTS[i];
                return;
            }
        }
    }

    public static int accentOf(int idx) {
        return THEME_ACCENTS[idx];
    }

    public static int accent() { return currentAccent; }

    public static int accent(int alpha) { return withAlpha(currentAccent, alpha); }

    public static int withAlpha(int color, int alpha) {
        return (clamp255(alpha) << 24) | (color & 0x00FFFFFF);
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    public static void fillRounded(GuiGraphicsExtractor context, int x, int y, int w, int h, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r == 0) { context.fill(x, y, x + w, y + h, color); return; }
        int baseAlpha = (color >>> 24) & 0xFF;

        context.fill(x + r, y, x + w - r, y + h, color);
        context.fill(x, y + r, x + r, y + h - r, color);
        context.fill(x + w - r, y + r, x + w, y + h - r, color);

        final int SUB = 4;

        for (int i = 0; i < r; i++) {
            double minReach = Double.MAX_VALUE, maxReach = -Double.MAX_VALUE;
            for (int s = 0; s < SUB; s++) {
                double subY = i + (s + 0.5) / SUB;
                double dist = r - subY;
                double reachF = Math.sqrt(Math.max(0.0, (double) r * r - dist * dist));
                minReach = Math.min(minReach, reachF);
                maxReach = Math.max(maxReach, reachF);
            }
            int solidReach = (int) Math.floor(minReach);
            int edgeEnd = (int) Math.ceil(maxReach);

            if (solidReach > 0) {
                context.fill(x + r - solidReach, y + i, x + r, y + i + 1, color);
                context.fill(x + w - r, y + i, x + w - r + solidReach, y + i + 1, color);
                context.fill(x + r - solidReach, y + h - 1 - i, x + r, y + h - i, color);
                context.fill(x + w - r, y + h - 1 - i, x + w - r + solidReach, y + h - i, color);
            }

            for (int c = solidReach; c < edgeEnd; c++) {
                int covered = 0;
                for (int s = 0; s < SUB; s++) {
                    double subY = i + (s + 0.5) / SUB;
                    double dist = r - subY;
                    double reachF = Math.sqrt(Math.max(0.0, (double) r * r - dist * dist));
                    if (reachF > c) covered++;
                }
                if (covered == 0) continue;
                int aaColor = withAlpha(color, Math.round(baseAlpha * (covered / (float) SUB)));
                context.fill(x + r - c - 1, y + i, x + r - c, y + i + 1, aaColor);
                context.fill(x + w - r + c, y + i, x + w - r + c + 1, y + i + 1, aaColor);
                context.fill(x + r - c - 1, y + h - 1 - i, x + r - c, y + h - i, aaColor);
                context.fill(x + w - r + c, y + h - 1 - i, x + w - r + c + 1, y + h - i, aaColor);
            }
        }
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor context, int x, int y, int w, int h, int radius, int color) {
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r <= 1) {
            context.outline(x, y, w, h, color);
            return;
        }
        context.fill(x + r, y, x + w - r, y + 1, color);
        context.fill(x + r, y + h - 1, x + w - r, y + h, color);
        context.fill(x, y + r, x + 1, y + h - r, color);
        context.fill(x + w - 1, y + r, x + w, y + h - r, color);

        int prevReach = 0;
        for (int i = 0; i < r; i++) {
            double dist = r - (i + 0.5);
            int reach = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - dist * dist)));
            int span = Math.max(1, prevReach > 0 ? Math.abs(reach - prevReach) : 1);
            int start = Math.max(0, reach - span);

            context.fill(x + r - reach, y + i, x + r - start, y + i + 1, color);
            context.fill(x + w - r + start, y + i, x + w - r + reach, y + i + 1, color);
            context.fill(x + r - reach, y + h - 1 - i, x + r - start, y + h - i, color);
            context.fill(x + w - r + start, y + h - 1 - i, x + w - r + reach, y + h - i, color);
            prevReach = reach;
        }
    }

    public static void fillRoundedCard(GuiGraphicsExtractor context, int x, int y, int w, int h, int radius, int fillColor, int borderColor) {
        fillRounded(context, x, y, w, h, radius, fillColor);
        drawRoundedOutline(context, x, y, w, h, radius, borderColor);
    }

    public static final int WINDOW_BG        = argb(232, 16, 16, 18);
    public static final int SIDEBAR_BG       = argb(210, 12, 12, 14);
    public static final int HEADER_BG        = argb(210, 14, 14, 16);
    public static final int PANEL_BG         = argb(242, 20, 20, 23);
    public static final int CARD_FILL        = argb(195, 28, 28, 32);
    public static final int CARD_HEADER_FILL = argb(215, 34, 34, 39);
    public static final int CARD_BORDER      = argb(28, 255, 255, 255);
    public static final int BORDER           = argb(20, 255, 255, 255);
    public static final int TEXT             = 0xFFF2F2F4;
    public static final int TEXT_DIM         = 0xFF94949E;
    public static final int FIELD_BG         = argb(220, 14, 14, 16);
    public static final int FIELD_BG_HOVER   = argb(235, 22, 22, 26);
    public static final int BUTTON_BG        = argb(215, 26, 26, 30);
    public static final int BUTTON_BG_HOVER  = argb(235, 36, 36, 42);
    public static final int DELETE_BG        = argb(200, 110, 24, 24);
    public static final int DELETE_BG_HOVER  = argb(235, 165, 36, 36);
}