package net.tse.ui;

import org.lwjgl.glfw.GLFW;

import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class PreciseSlider extends AbstractWidget {

    public interface Formatter { String format(double value); }

    private double value;
    private final double min, max, step;
    private final Formatter formatter;
    private final DoubleConsumer onChange;
    private final String label;

    private boolean editing = false;
    private final StringBuilder editBuffer = new StringBuilder();

    public PreciseSlider(int x, int y, int width, double initial, double min, double max,
                         double step, Formatter formatter, DoubleConsumer onChange) {
        this(x, y, width, initial, min, max, step, formatter, onChange, null);
    }

    public PreciseSlider(int x, int y, int width, double initial, double min, double max,
                         double step, Formatter formatter, DoubleConsumer onChange, String label) {
        super(x, y, width, 16, Component.empty());
        this.min = min; this.max = max; this.step = step;
        this.value = clamp(initial);
        this.formatter = formatter;
        this.onChange = onChange;
        this.label = label;
    }

    public static final int LABEL_HEIGHT = 10;

    public double get() { return value; }

    private double clamp(double v) {
        double c = Math.max(min, Math.min(max, v));
        if (step > 0) c = Math.round(c / step) * step;
        return c;
    }

    private static final int VALUE_ZONE_W = 48;

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x();
        int x = getX(), w = getWidth();
        if (mouseX >= x + w - VALUE_ZONE_W) {
            beginEdit();
            return;
        }
        editing = false;
        dragTo(mouseX);
    }

    @Override
    protected void onDrag(MouseButtonEvent click, double deltaX, double deltaY) {
        if (!editing) dragTo(click.x());
    }

    private void dragTo(double mouseX) {
        int x = getX(), w = getWidth() - VALUE_ZONE_W - 6;
        double t = (mouseX - x) / (double) Math.max(1, w);
        t = Math.max(0, Math.min(1, t));
        double newVal = clamp(min + t * (max - min));
        if (newVal != value) {
            value = newVal;
            if (onChange != null) onChange.accept(value);
        }
    }

    private void beginEdit() {
        editing = true;
        editBuffer.setLength(0);
    }

    private void commitEdit() {
        try {
            double typed = Double.parseDouble(editBuffer.toString().trim());
            value = clamp(typed);
            if (onChange != null) onChange.accept(value);
        } catch (NumberFormatException ignored) {}
        editing = false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (!editing) return false;
        int keyCode = input.key();
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            commitEdit();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            editing = false;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE && editBuffer.length() > 0) {
            editBuffer.deleteCharAt(editBuffer.length() - 1);
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!editing) return false;
        char chr = (char) input.codepoint();
        if (Character.isDigit(chr) || chr == '.' || chr == '-') {
            editBuffer.append(chr);
        }
        return true;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        var mc = Minecraft.getInstance();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int trackW = w - VALUE_ZONE_W - 6;

        if (label != null && !label.isEmpty()) {
            MCTheme.drawTextHD(context, mc.font, label, x, y - LABEL_HEIGHT, MCTheme.TEXT_DIM, false);
        }

        int trackY = y + h / 2 - 2;
        MCTheme.fillRoundedCard(context, x, trackY, trackW, 5, 2, MCTheme.FIELD_BG, MCTheme.BORDER);
        double t = max > min ? (value - min) / (max - min) : 0;
        int fillW = (int) Math.round(trackW * t);
        if (fillW > 0) {
            MCTheme.fillRounded(context, x, trackY, Math.max(4, fillW), 5, 2, MCTheme.accent(230));
        }

        int knobX = x + fillW;
        int knobR = isHovered() ? 5 : 4;
        MCTheme.fillRounded(context, knobX - knobR, y + h / 2 - knobR, knobR * 2 + 1, knobR * 2 + 1, knobR, 0xFFFFFFFF);

        int badgeX = x + trackW + 6;
        int badgeW = VALUE_ZONE_W;
        int badgeBorder = editing ? MCTheme.accent(200) : MCTheme.BORDER;
        MCTheme.fillRoundedCard(context, badgeX, y + 1, badgeW, h - 2, 3, MCTheme.FIELD_BG, badgeBorder);

        String text = editing ? (editBuffer + "_") : formatter.format(value);
        int textColor = editing ? MCTheme.accent(255) : MCTheme.TEXT;
        int textW = mc.font.width(text);
        MCTheme.drawTextHD(context, mc.font, text, badgeX + (badgeW - textW) / 2, y + (h - 8) / 2, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.literal(formatter.format(value)));
    }
}