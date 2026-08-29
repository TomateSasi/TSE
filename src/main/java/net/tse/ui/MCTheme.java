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
            argb(255, 220, 40, 40),   // Default - red
            argb(255, 50, 130, 255),  // Blue
            argb(255, 40, 185, 80),   // Green
            argb(255, 155, 60, 255),  // Purple
            argb(255, 215, 96, 20),   // Orange
            argb(255, 230, 80, 160),  // Pink
            argb(255, 30, 200, 220),  // Cyan
            argb(255, 220, 175, 0),   // Gold
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

    public static final int WINDOW_BG   = argb(235, 16, 16, 16);
    public static final int PANEL_BG    = argb(255, 20, 20, 20);
    public static final int CARD_FILL   = argb(255, 32, 32, 32);
    public static final int CARD_BORDER = argb(24, 255, 255, 255);
    public static final int BORDER      = argb(15, 255, 255, 255);
    public static final int TEXT        = 0xFFFFFFFF;
    public static final int TEXT_DIM    = 0xFF8B8B8B;
    public static final int FIELD_BG    = argb(255, 16, 16, 16);
    public static final int FIELD_BG_HOVER = argb(255, 26, 26, 26);
    public static final int BUTTON_BG        = argb(255, 26, 26, 26);
    public static final int BUTTON_BG_HOVER  = argb(255, 33, 33, 33);
    public static final int DELETE_BG        = argb(255, 122, 20, 20);
    public static final int DELETE_BG_HOVER  = argb(255, 178, 36, 36);
    public static final int SIDEBAR_BG       = argb(255, 12, 12, 12);
}