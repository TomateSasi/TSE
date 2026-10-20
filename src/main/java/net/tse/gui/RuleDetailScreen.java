package net.tse.gui;

import net.tse.ui.FlatTextField;
import net.tse.ui.FlatButton;
import com.example.ModConfig;
import com.example.tse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.tse.ui.ColorPickerScreen;
import net.tse.ui.MCTheme;
import net.tse.ui.PreciseSlider;
import net.tse.ui.ToggleWidget;

import java.util.ArrayList;
import java.util.List;

public class RuleDetailScreen extends Screen {

    private static final int LABEL_W = 88;
    private static final int ROW_H = 22;

    private final Screen parent;
    private final ModConfig.SoundRule rule;

    private int winX, winY, winW, winH;
    private int left;
    private int controlX;
    private int cursorY;

    private int card1Y, card1H;
    private int card2Y, card2H;

    private final List<Object[]> labels = new ArrayList<>();
    private final List<Object[]> sectionHeaders = new ArrayList<>();

    public RuleDetailScreen(Screen parent, ModConfig.SoundRule rule) {
        super(Component.literal("Rule Overlay Settings"));
        this.parent = parent;
        this.rule = rule;
    }

    @Override
    protected void init() {
        clearWidgets();
        labels.clear();
        sectionHeaders.clear();

        winW = Math.min(width - 24, 460);
        winH = Math.min(height - 20, 396);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        left = winX + 18;
        controlX = left + LABEL_W;
        cursorY = winY + 38;

        card1Y = cursorY;
        cursorY += 8;
        sectionHeader("Trigger Overlay (shown when sound plays)", MCTheme.accent(255));
        row("Enabled", () -> new ToggleWidget(controlX, rowMid(), rule.overlayEnabled,
                v -> { rule.overlayEnabled = v; tse.saveConfig(); }));
        row("Text", () -> {
            EditBox f = new FlatTextField(font, controlX, cursorY, 240, 18, Component.literal("Overlay text"));
            f.setMaxLength(128);
            f.setValue(rule.overlayText);
            f.setResponder(s -> { rule.overlayText = s; tse.saveConfig(); });
            return f;
        });
        row("Rainbow", () -> new ToggleWidget(controlX, rowMid(), rule.overlayRainbow,
                v -> { rule.overlayRainbow = v; tse.saveConfig(); }));
        widget(FlatButton.builder(Component.literal("Edit Color"), btn ->
                        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(this, rule.overlayColor,
                                c -> { rule.overlayColor = c; tse.saveConfig(); })))
                .dimensions(controlX + 90, cursorY - ROW_H, 76, 18).build());
        row("Scale", () -> new PreciseSlider(controlX, cursorY, 200, rule.overlayScale, 0.5, 8.0, 0.1,
                v -> String.format("%.1fx", v), v -> { rule.overlayScale = (float) v; tse.saveConfig(); }));
        row("Duration", () -> new PreciseSlider(controlX, cursorY, 200, rule.overlayDurationMs, 200, 10000, 100,
                v -> String.format("%.0fms", v), v -> { rule.overlayDurationMs = (int) v; tse.saveConfig(); }));
        row("Position", () -> null);
        addPositionControls(rule.overlayX, rule.overlayY, (x2, y2) -> {
            rule.overlayX = x2; rule.overlayY = y2; tse.saveConfig();
        });
        card1H = (cursorY + 4) - card1Y;

        cursorY += 10;
        card2Y = cursorY;
        cursorY += 8;
        sectionHeader("Close Warning (shown before sound plays)", 0xFFFF7777);
        row("Enabled", () -> new ToggleWidget(controlX, rowMid(), rule.closeWarnEnabled,
                v -> { rule.closeWarnEnabled = v; tse.saveConfig(); }));
        row("Text", () -> {
            EditBox f = new FlatTextField(font, controlX, cursorY, 240, 18, Component.literal("Warning text"));
            f.setMaxLength(128);
            f.setValue(rule.closeWarnText);
            f.setResponder(s -> { rule.closeWarnText = s; tse.saveConfig(); });
            return f;
        });
        row("Rainbow", () -> new ToggleWidget(controlX, rowMid(), rule.closeWarnRainbow,
                v -> { rule.closeWarnRainbow = v; tse.saveConfig(); }));
        widget(new ToggleWidget(controlX + 60, cursorY - ROW_H + 3, rule.closeWarnBlink, v -> { rule.closeWarnBlink = v; tse.saveConfig(); }));
        label(controlX + 88, cursorY - ROW_H + 5, "Blink");
        widget(FlatButton.builder(Component.literal("Edit Color"), btn ->
                        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(this, rule.closeWarnColor,
                                c -> { rule.closeWarnColor = c; tse.saveConfig(); })))
                .dimensions(controlX + 140, cursorY - ROW_H, 76, 18).build());
        row("Scale", () -> new PreciseSlider(controlX, cursorY, 200, rule.closeWarnScale, 0.5, 8.0, 0.1,
                v -> String.format("%.1fx", v), v -> { rule.closeWarnScale = (float) v; tse.saveConfig(); }));
        row("Warn Before", () -> new PreciseSlider(controlX, cursorY, 200, rule.closeWarnSecondsBeforeEnd, 1, 60, 1,
                v -> String.format("%.0fs", v), v -> { rule.closeWarnSecondsBeforeEnd = (int) v; tse.saveConfig(); }));
        row("Position", () -> null);
        addPositionControls(rule.closeWarnX, rule.closeWarnY, (x2, y2) -> {
            rule.closeWarnX = x2; rule.closeWarnY = y2; tse.saveConfig();
        });
        card2H = (cursorY + 4) - card2Y;

        int footerY = winY + winH - 28;
        widget(FlatButton.builder(Component.literal("Visual HUD Editor..."), btn ->
                        Minecraft.getInstance().gui.setScreen(new TSEHudEditorScreen(this, rule)))
                .dimensions(winX + 14, footerY, 130, 20)
                .style(FlatButton.Style.ACCENT)
                .build());
        widget(FlatButton.builder(Component.literal("Done"), btn -> onClose())
                .dimensions(winX + winW - 104, footerY, 90, 20)
                .build());
    }

    private void row(String labelText, java.util.function.Supplier<AbstractWidget> widgetFactory) {
        label(left, cursorY + 5, labelText);
        AbstractWidget w = widgetFactory.get();
        if (w != null) addRenderableWidget(w);
        cursorY += ROW_H;
    }

    private void widget(AbstractWidget w) {
        addRenderableWidget(w);
    }

    private void label(int x, int y, String text) {
        labels.add(new Object[]{text, x, y});
    }

    private void sectionHeader(String text, int color) {
        sectionHeaders.add(new Object[]{text, cursorY, color});
        cursorY += 16;
    }

    private int rowMid() { return cursorY + 3; }

    private void addPositionControls(int gx, int gy, PosSetter setter) {
        int rowY = cursorY - ROW_H;
        boolean centered = gx < 0;
        widget(new ToggleWidget(controlX, rowY + 3, centered, v -> {
            setter.set(v ? -1 : 0, gy);
            init();
        }));
        label(controlX + 28, rowY + 5, "Center");
        int fx = controlX + 76;
        if (!centered) {
            EditBox xField = new FlatTextField(font, fx, rowY, 54, 18, Component.literal("X"));
            xField.setValue(String.valueOf(gx));
            xField.setResponder(s -> {
                if (!s.isEmpty() && !s.matches("-?\\d*")) { xField.setValue(s.replaceAll("[^-\\d]", "")); return; }
                try { setter.set(Integer.parseInt(s), gy); } catch (Exception ignored) {}
            });
            widget(xField);
            fx += 60;
        }
        EditBox yField = new FlatTextField(font, fx, rowY, 54, 18, Component.literal("Y"));
        yField.setValue(String.valueOf(gy));
        yField.setResponder(s -> {
            if (!s.isEmpty() && !s.matches("-?\\d*")) { yField.setValue(s.replaceAll("[^-\\d]", "")); return; }
            try { setter.set(centered ? -1 : gx, Integer.parseInt(s)); } catch (Exception ignored) {}
        });
        widget(yField);
    }

    private interface PosSetter { void set(int x, int y); }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parent != null) parent.extractRenderState(context, -1, -1, delta);
        context.fill(0, 0, width, height, 0x88000000);

        MCTheme.fillRoundedCard(context, winX, winY, winW, winH, 12, MCTheme.WINDOW_BG, MCTheme.CARD_BORDER);

        String itemTitle = rule.itemKeyword.isEmpty() ? "Sound Rule" : rule.itemKeyword;
        String title = "Overlay & Warning Settings — " + itemTitle;
        MCTheme.drawTextHD(context, font, title, winX + 16, winY + 12, MCTheme.TEXT, false);
        context.fill(winX + 12, winY + 28, winX + winW - 12, winY + 29, MCTheme.BORDER);

        MCTheme.fillRoundedCard(context, winX + 12, card1Y, winW - 24, card1H, 6, MCTheme.CARD_FILL, MCTheme.CARD_BORDER);
        MCTheme.fillRoundedCard(context, winX + 12, card2Y, winW - 24, card2H, 6, MCTheme.CARD_FILL, MCTheme.CARD_BORDER);

        for (Object[] s : sectionHeaders) {
            MCTheme.drawTextHD(context, font, (String) s[0], left, (int) s[1], (int) s[2], false);
        }
        for (Object[] l : labels) {
            MCTheme.drawTextHD(context, font, (String) l[0], (int) l[1], (int) l[2], MCTheme.TEXT_DIM, false);
        }

        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }
}