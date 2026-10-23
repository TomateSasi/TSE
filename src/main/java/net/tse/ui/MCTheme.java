package net.tse.ui;

import com.example.tse;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class MCTheme {

    private MCTheme() {}

    public static final Identifier FONT = Identifier.fromNamespaceAndPath("minecraft", "default");
    private static final Style FONT_STYLE = Style.EMPTY.withFont(new FontDescription.Resource(FONT));

    public static Component styled(String s) {
        return Component.literal(s).setStyle(FONT_STYLE);
    }

    public static void drawTextHD(GuiGraphicsExtractor context, Font textRenderer,
                                  String s, int x, int y, int color, boolean shadow) {
        context.text(textRenderer, styled(s), x, y, color, shadow);
    }

    public static final String[] THEME_NAMES = {
            "Default", "Blue", "Green", "Purple", "Orange", "Pink", "Cyan", "Gold", "Custom"
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
        if ("Custom".equalsIgnoreCase(name)) {
            int custom = (tse.config != null && tse.config.customThemeColor != 0)
                    ? tse.config.customThemeColor
                    : THEME_ACCENTS[0];
            currentAccent = withAlpha(custom, 255);
            return;
        }
        for (int i = 0; i < THEME_ACCENTS.length; i++) {
            if (THEME_NAMES[i].equalsIgnoreCase(name)) {
                currentAccent = THEME_ACCENTS[i];
                return;
            }
        }
    }

    public static void setCustomAccent(int argbColor) {
        currentAccent = withAlpha(argbColor, 255);
    }

    public static int accentOf(int idx) {
        if (idx >= 0 && idx < THEME_ACCENTS.length) return THEME_ACCENTS[idx];
        return currentAccent;
    }

    public static int accent() { return currentAccent; }

    public static int accent(int alpha) { return withAlpha(currentAccent, alpha); }

    public static int withAlpha(int color, int alpha) {
        return (clamp255(alpha) << 24) | (color & 0x00FFFFFF);
    }

    public static int argb(int a, int r, int g, int b) {
        return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
    }

    public static int lerpColor(int c1, int c2, float t) {
        float f = Math.max(0f, Math.min(1f, t));
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        return argb(
                Math.round(a1 + (a2 - a1) * f),
                Math.round(r1 + (r2 - r1) * f),
                Math.round(g1 + (g2 - g1) * f),
                Math.round(b1 + (b2 - b1) * f)
        );
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    public static void fillRounded(GuiGraphicsExtractor context, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha == 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r <= 1) {
            context.fill(x, y, x + w, y + h, color);
            return;
        }

        context.fill(x, y + r, x + w, y + h - r, color);

        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int reach = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            int inset = Math.max(0, r - reach);
            context.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            context.fill(x + inset, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void drawRoundedOutline(GuiGraphicsExtractor context, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int baseAlpha = (color >>> 24) & 0xFF;
        if (baseAlpha == 0) return;
        int r = Math.max(0, Math.min(radius, Math.min(w, h) / 2));
        if (r <= 1) {
            context.outline(x, y, w, h, color);
            return;
        }

        context.fill(x + r, y, x + w - r, y + 1, color);
        context.fill(x + r, y + h - 1, x + w - r, y + h, color);
        context.fill(x, y + r, x + 1, y + h - r, color);
        context.fill(x + w - 1, y + r, x + w, y + h - r, color);

        for (int i = 1; i < r; i++) {
            double dy = r - i - 0.5;
            int reach = (int) Math.round(Math.sqrt(Math.max(0.0, (double) r * r - dy * dy)));
            int inset = Math.max(0, r - reach);
            context.fill(x + inset, y + i, x + inset + 1, y + i + 1, color);
            context.fill(x + w - inset - 1, y + i, x + w - inset, y + i + 1, color);
            context.fill(x + inset, y + h - 1 - i, x + inset + 1, y + h - i, color);
            context.fill(x + w - inset - 1, y + h - 1 - i, x + w - inset, y + h - i, color);
        }
    }

    public static void fillRoundedCard(GuiGraphicsExtractor context, int x, int y, int w, int h, int radius, int fillColor, int borderColor) {
        fillRounded(context, x, y, w, h, radius, fillColor);
        drawRoundedOutline(context, x, y, w, h, radius, borderColor);
    }

    private static class ToastEntry {
        final String message;
        final long createdMs;
        final long durationMs = 2200L;

        ToastEntry(String message) {
            this.message = message;
            this.createdMs = System.currentTimeMillis();
        }
    }

    private static final List<ToastEntry> TOASTS = new ArrayList<>();
    private static long lastDingMs = 0L;

    public static void showToast(String message) {
        showToast(message, true);
    }

    public static void showToast(String message, boolean playDingSound) {
        synchronized (TOASTS) {
            if (TOASTS.size() >= 4) TOASTS.remove(0);
            TOASTS.add(new ToastEntry(message));
        }
        if (playDingSound) {
            playDing();
        }
    }

    public static void playDing() {
        long now = System.currentTimeMillis();
        if (now - lastDingMs < 60L) return;
        lastDingMs = now;

        Thread t = new Thread(() -> {
            try {
                float sampleRate = 44100f;
                int durationMs = 150;
                int totalSamples = (int) (sampleRate * durationMs / 1000f);
                byte[] pcm = new byte[totalSamples * 2];

                for (int i = 0; i < totalSamples; i++) {
                    double time = i / (double) sampleRate;
                    double freq = i < totalSamples * 0.35 ? 987.77 : 1318.51;
                    double attack = Math.min(1.0, i / (sampleRate * 0.006));
                    double decay = Math.exp(-time * 22.0);
                    double env = attack * decay;
                    double wave = Math.sin(2.0 * Math.PI * freq * time) * 0.75
                            + Math.sin(2.0 * Math.PI * (freq * 2.0) * time) * 0.25;
                    short sample = (short) (wave * env * 6500);
                    pcm[i * 2] = (byte) (sample & 0xFF);
                    pcm[i * 2 + 1] = (byte) ((sample >>> 8) & 0xFF);
                }

                AudioFormat fmt = new AudioFormat(sampleRate, 16, 1, true, false);
                DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
                if (!AudioSystem.isLineSupported(info)) return;
                SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
                line.open(fmt);
                line.start();
                line.write(pcm, 0, pcm.length);
                line.drain();
                line.close();
            } catch (Exception ignored) {}
        }, "TSE-UI-Ding");
        t.setDaemon(true);
        t.start();
    }

    public static void renderToasts(GuiGraphicsExtractor context, Font font, int screenW, int screenH) {
        long now = System.currentTimeMillis();
        synchronized (TOASTS) {
            Iterator<ToastEntry> it = TOASTS.iterator();
            int idx = 0;
            while (it.hasNext()) {
                ToastEntry toast = it.next();
                long age = now - toast.createdMs;
                if (age >= toast.durationMs) {
                    it.remove();
                    continue;
                }

                float inAnim = Math.min(1f, age / 140f);
                float outAnim = age > toast.durationMs - 260L ? Math.max(0f, (toast.durationMs - age) / 260f) : 1f;
                float vis = inAnim * outAnim;

                int textW = font.width(toast.message);
                int boxW = textW + 22;
                int boxH = 22;
                int x = screenW - boxW - 12;
                int y = screenH - 32 - idx * 26;

                int alpha = Math.max(0, Math.min(255, (int) (235 * vis)));
                int borderAlpha = Math.max(0, Math.min(255, (int) (140 * vis)));
                int textAlpha = Math.max(5, Math.min(255, (int) (255 * vis)));

                fillRoundedCard(context, x, y, boxW, boxH, 4,
                        withAlpha(PANEL_BG, alpha),
                        accent(borderAlpha));
                fillRounded(context, x + 4, y + 5, 2, boxH - 10, 1, accent(textAlpha));
                drawTextHD(context, font, toast.message, x + 11, y + 7, withAlpha(TEXT, textAlpha), false);
                idx++;
            }
        }
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