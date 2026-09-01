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

    private static final int LABEL_W = 96;
    private static final int ROW_H = 24;

    private final Screen parent;
    private final ModConfig.SoundRule rule;

    private int left;
    private int controlX;
    private int cursorY;

    private final List<Object[]> labels = new ArrayList<>();
    private final List<Object[]> sectionHeaders = new ArrayList<>();

    public RuleDetailScreen(Screen parent, ModConfig.SoundRule rule) {
        super(Component.literal("Rule Overlay Settings"));
        this.parent = parent;
        this.rule = rule;
    }

    @Override
    protected void init() {
        labels.clear();
        sectionHeaders.clear();

        left = width / 2 - 220;
        controlX = left + LABEL_W;
        cursorY = height / 2 - 150;

        sectionHeader("Trigger overlay (shown when sound plays)", MCTheme.accent(255));
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
        widget(FlatButton.builder(Component.literal("Color"), btn ->
                        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(this, rule.overlayColor,
                                c -> { rule.overlayColor = c; tse.saveConfig(); })))
                .dimensions(controlX + 110, cursorY, 66, 18).build());
        cursorY += ROW_H;
        row("Scale", () -> new PreciseSlider(controlX, cursorY, 190, rule.overlayScale, 0.5, 8.0, 0.1,
                v -> String.format("%.1fx", v), v -> { rule.overlayScale = (float) v; tse.saveConfig(); }));
        row("Show (ms)", () -> new PreciseSlider(controlX, cursorY, 190, rule.overlayDurationMs, 200, 10000, 100,
                v -> String.format("%.0fms", v), v -> { rule.overlayDurationMs = (int) v; tse.saveConfig(); }));
        row("Position", () -> null);
        addPositionControls(rule.overlayX, rule.overlayY, (x2, y2) -> { rule.overlayX = x2; rule.overlayY = y2; tse.saveConfig(); });
        cursorY += ROW_H + 12;

        sectionHeader("Close warning (shown before sound plays)", 0xFFFF6666);
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
        widget(new ToggleWidget(controlX + 70, rowMid(), rule.closeWarnBlink, v -> { rule.closeWarnBlink = v; tse.saveConfig(); }));
        label(controlX + 96, cursorY + 5, "Blink");
        widget(FlatButton.builder(Component.literal("Color"), btn ->
                        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(this, rule.closeWarnColor,
                                c -> { rule.closeWarnColor = c; tse.saveConfig(); })))
                .dimensions(controlX + 160, cursorY, 66, 18).build());
        cursorY += ROW_H;
        row("Scale", () -> new PreciseSlider(controlX, cursorY, 190, rule.closeWarnScale, 0.5, 8.0, 0.1,
                v -> String.format("%.1fx", v), v -> { rule.closeWarnScale = (float) v; tse.saveConfig(); }));
        row("Warn at (sec)", () -> new PreciseSlider(controlX, cursorY, 190, rule.closeWarnSecondsBeforeEnd, 1, 60, 1,
                v -> String.format("%.0fs", v), v -> { rule.closeWarnSecondsBeforeEnd = (int) v; tse.saveConfig(); }));
        row("Position", () -> null);
        addPositionControls(rule.closeWarnX, rule.closeWarnY, (x2, y2) -> { rule.closeWarnX = x2; rule.closeWarnY = y2; tse.saveConfig(); });
        cursorY += ROW_H + 20;

        widget(FlatButton.builder(Component.literal("Done"), btn -> onClose())
                .dimensions(width / 2 - 60, cursorY, 120, 20).build());
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
        cursorY += 18;
    }

    private int rowMid() { return cursorY + 3; }

    private void addPositionControls(int gx, int gy, PosSetter setter) {
        boolean centered = gx < 0;
        widget(new ToggleWidget(controlX, rowMid(), centered, v -> setter.set(v ? -1 : 0, gy)));
        int fx = controlX + 30;
        if (!centered) {
            EditBox xField = new FlatTextField(font, fx, cursorY, 60, 18, Component.literal("X"));
            xField.setValue(String.valueOf(gx));
            xField.setResponder(s -> {
                if (!s.isEmpty() && !s.matches("-?\\d*")) { xField.setValue(s.replaceAll("[^-\\d]", "")); return; }
                try { setter.set(Integer.parseInt(s), gy); } catch (Exception ignored) {}
            });
            widget(xField);
            fx += 68;
        }
        EditBox yField = new FlatTextField(font, fx, cursorY, 60, 18, Component.literal("Y"));
        yField.setValue(String.valueOf(gy));
        yField.setResponder(s -> {
            if (!s.isEmpty() && !s.matches("-?\\d*")) { yField.setValue(s.replaceAll("[^-\\d]", "")); return; }
            try { setter.set(centered ? -1 : gx, Integer.parseInt(s)); } catch (Exception ignored) {}
        });
        widget(yField);
        cursorY += ROW_H;
    }

    private interface PosSetter { void set(int x, int y); }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0xC0000000);

        String title = "Overlay / Warning settings — " + rule.itemKeyword;
        int titleW = font.width(title);
        MCTheme.drawTextHD(context, font, title, width / 2 - titleW / 2, height / 2 - 175, MCTheme.TEXT, true);

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
