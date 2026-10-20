package net.tse.gui;

import com.example.ModConfig;
import com.example.tse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.tse.ui.MCTheme;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class TSEHudEditorScreen extends Screen {

    private static final int HIT_PAD = 6;
    private static final float SCALE_STEP = 0.1f;
    private static final float SCALE_MIN = 0.5f;
    private static final float SCALE_MAX = 8.0f;

    private final Screen parent;
    private final ModConfig.SoundRule targetRule;

    private static class HudBox {
        final String title;
        final java.util.function.Supplier<String> textSupplier;
        final java.util.function.IntSupplier colorSupplier;
        final java.util.function.IntSupplier xGetter;
        final java.util.function.IntSupplier yGetter;
        final java.util.function.Supplier<Float> scaleGetter;
        final java.util.function.BiConsumer<Integer, Integer> posSetter;
        final java.util.function.Consumer<Float> scaleSetter;
        final int defaultY;
        final float defaultScale;

        HudBox(String title,
               java.util.function.Supplier<String> textSupplier,
               java.util.function.IntSupplier colorSupplier,
               java.util.function.IntSupplier xGetter,
               java.util.function.IntSupplier yGetter,
               java.util.function.Supplier<Float> scaleGetter,
               java.util.function.BiConsumer<Integer, Integer> posSetter,
               java.util.function.Consumer<Float> scaleSetter,
               int defaultY,
               float defaultScale) {
            this.title = title;
            this.textSupplier = textSupplier;
            this.colorSupplier = colorSupplier;
            this.xGetter = xGetter;
            this.yGetter = yGetter;
            this.scaleGetter = scaleGetter;
            this.posSetter = posSetter;
            this.scaleSetter = scaleSetter;
            this.defaultY = defaultY;
            this.defaultScale = defaultScale;
        }
    }

    private final List<HudBox> boxes = new ArrayList<>();
    private HudBox selected = null;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;

    public TSEHudEditorScreen(Screen parent, ModConfig.SoundRule rule) {
        super(Component.literal("TSE HUD Editor"));
        this.parent = parent;
        this.targetRule = rule;
    }

    @Override
    protected void init() {
        boxes.clear();
        ModConfig.VoidgloomSettings vg = tse.config.voidgloom;

        boxes.add(new HudBox(
                "Voidgloom Overlay",
                () -> vg.overlayText == null || vg.overlayText.isEmpty() ? "&cSNEAK!" : vg.overlayText,
                () -> vg.overlayColor,
                () -> vg.overlayX,
                () -> vg.overlayY,
                () -> vg.overlayScale,
                (x, y) -> { vg.overlayX = x; vg.overlayY = y; },
                s -> vg.overlayScale = s,
                80, 2.0f
        ));

        if (targetRule != null) {
            String name = targetRule.itemKeyword.isEmpty() ? "Rule" : targetRule.itemKeyword;
            boxes.add(new HudBox(
                    "Trigger Overlay (" + name + ")",
                    () -> targetRule.overlayText == null || targetRule.overlayText.isEmpty() ? "NOW!" : targetRule.overlayText,
                    () -> targetRule.overlayColor,
                    () -> targetRule.overlayX,
                    () -> targetRule.overlayY,
                    () -> targetRule.overlayScale,
                    (x, y) -> { targetRule.overlayX = x; targetRule.overlayY = y; },
                    s -> targetRule.overlayScale = s,
                    80, 2.0f
            ));
            boxes.add(new HudBox(
                    "Close Warning (" + name + ")",
                    () -> targetRule.closeWarnText == null || targetRule.closeWarnText.isEmpty() ? "GET READY!" : targetRule.closeWarnText,
                    () -> targetRule.closeWarnColor,
                    () -> targetRule.closeWarnX,
                    () -> targetRule.closeWarnY,
                    () -> targetRule.closeWarnScale,
                    (x, y) -> { targetRule.closeWarnX = x; targetRule.closeWarnY = y; },
                    s -> targetRule.closeWarnScale = s,
                    60, 1.5f
            ));
        }

        if (!boxes.isEmpty() && selected == null) {
            selected = targetRule != null && boxes.size() > 1 ? boxes.get(1) : boxes.get(0);
        }
    }

    private String cleanText(String raw) {
        return raw.replace('&', '\u00a7');
    }

    private int[] computeBounds(HudBox box) {
        float scale = box.scaleGetter.get();
        String text = cleanText(box.textSupplier.get());
        int textW = Math.max(16, (int) (font.width(text) * scale));
        int textH = Math.max(8, (int) (font.lineHeight * scale));
        int rawX = box.xGetter.getAsInt();
        int x = rawX < 0 ? (width - textW) / 2 : rawX;
        int y = box.yGetter.getAsInt();
        return new int[]{x, y, textW, textH};
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x88000000);

        context.fill(width / 2, 0, width / 2 + 1, height, 0x22FFFFFF);
        context.fill(0, height / 2, width, height / 2 + 1, 0x22FFFFFF);

        for (HudBox box : boxes) {
            int[] b = computeBounds(box);
            int bx = b[0], by = b[1], bw = b[2], bh = b[3];
            boolean isSel = box == selected;
            boolean isHov = !dragging && isInside(mouseX, mouseY, bx, by, bw, bh);

            int fill = isSel ? MCTheme.accent(32) : (isHov ? 0x28FFFFFF : 0x18000000);
            int border = isSel ? MCTheme.accent(230) : (isHov ? MCTheme.accent(120) : MCTheme.CARD_BORDER);
            MCTheme.fillRoundedCard(context, bx - HIT_PAD, by - HIT_PAD, bw + HIT_PAD * 2, bh + HIT_PAD * 2, 4, fill, border);

            tse.drawHudText(context, Minecraft.getInstance(), box.textSupplier.get(),
                    box.xGetter.getAsInt(), box.yGetter.getAsInt(), box.scaleGetter.get(), box.colorSupplier.getAsInt(), width);

            if (isSel || isHov) {
                String tag = box.title + (box.xGetter.getAsInt() < 0 ? " [Centered]" : "");
                int tagW = font.width(tag);
                int tagX = bx + (bw - tagW) / 2;
                int tagY = Math.max(4, by - HIT_PAD - 12);
                MCTheme.fillRounded(context, tagX - 4, tagY - 2, tagW + 8, 11, 3, MCTheme.WINDOW_BG);
                MCTheme.drawTextHD(context, font, tag, tagX, tagY, isSel ? MCTheme.accent(255) : MCTheme.TEXT, false);
            }
        }

        drawLegend(context);
        super.extractRenderState(context, mouseX, mouseY, delta);
    }

    private void drawLegend(GuiGraphicsExtractor context) {
        List<String> lines = new ArrayList<>();
        lines.add("TSE HUD Editor");
        if (selected != null) {
            lines.add("Selected: " + selected.title);
            String xStr = selected.xGetter.getAsInt() < 0 ? "Centered" : String.valueOf(selected.xGetter.getAsInt());
            lines.add("Pos: X=" + xStr + ", Y=" + selected.yGetter.getAsInt() + " | Scale: " + String.format("%.1fx", selected.scaleGetter.get()));
        } else {
            lines.add("Click an overlay to select");
        }
        lines.add("Drag: Move   |   Scroll: Scale");
        lines.add("Arrows: Nudge   |   C: Center X   |   R: Reset");
        lines.add("Esc: Save & Return");

        int maxW = 0;
        for (String s : lines) maxW = Math.max(maxW, font.width(s));
        int pad = 8;
        int lineH = 12;
        int boxW = maxW + pad * 2;
        int boxH = lines.size() * lineH + pad * 2 - 2;
        int boxX = width - boxW - 10;
        int boxY = height - boxH - 10;

        MCTheme.fillRoundedCard(context, boxX, boxY, boxW, boxH, 6, MCTheme.WINDOW_BG, MCTheme.CARD_BORDER);
        for (int i = 0; i < lines.size(); i++) {
            int col = i == 0 ? MCTheme.accent(255) : (i <= 2 ? MCTheme.TEXT : MCTheme.TEXT_DIM);
            MCTheme.drawTextHD(context, font, lines.get(i), boxX + pad, boxY + pad + i * lineH, col, false);
        }
    }

    private boolean isInside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x - HIT_PAD && mx <= x + w + HIT_PAD && my >= y - HIT_PAD && my <= y + h + HIT_PAD;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            for (int i = boxes.size() - 1; i >= 0; i--) {
                HudBox box = boxes.get(i);
                int[] b = computeBounds(box);
                if (isInside(click.x(), click.y(), b[0], b[1], b[2], b[3])) {
                    selected = box;
                    dragging = true;
                    dragOffsetX = (int) click.x() - b[0];
                    dragOffsetY = (int) click.y() - b[1];
                    return true;
                }
            }
            selected = null;
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (dragging && selected != null) {
            int[] b = computeBounds(selected);
            int newX = Math.max(0, Math.min(width - b[2], (int) click.x() - dragOffsetX));
            int newY = Math.max(0, Math.min(height - b[3], (int) click.y() - dragOffsetY));
            int centerDist = Math.abs((newX + b[2] / 2) - (width / 2));
            if (centerDist <= 6) {
                selected.posSetter.accept(-1, newY);
            } else {
                selected.posSetter.accept(newX, newY);
            }
            return true;
        }
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (dragging) {
            dragging = false;
            tse.saveConfig();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (selected != null) {
            float cur = selected.scaleGetter.get();
            float next = Math.round((cur + (float) verticalAmount * SCALE_STEP) * 10f) / 10f;
            next = Math.max(SCALE_MIN, Math.min(SCALE_MAX, next));
            selected.scaleSetter.accept(next);
            tse.saveConfig();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (selected != null) {
            int step = (input.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0 ? 5 : 1;
            int[] b = computeBounds(selected);
            int curX = selected.xGetter.getAsInt();
            int curY = selected.yGetter.getAsInt();
            switch (key) {
                case GLFW.GLFW_KEY_LEFT -> {
                    int nx = (curX < 0 ? b[0] : curX) - step;
                    selected.posSetter.accept(Math.max(0, nx), curY);
                    tse.saveConfig();
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    int nx = (curX < 0 ? b[0] : curX) + step;
                    selected.posSetter.accept(Math.min(width - b[2], nx), curY);
                    tse.saveConfig();
                    return true;
                }
                case GLFW.GLFW_KEY_UP -> {
                    selected.posSetter.accept(curX, Math.max(0, curY - step));
                    tse.saveConfig();
                    return true;
                }
                case GLFW.GLFW_KEY_DOWN -> {
                    selected.posSetter.accept(curX, Math.min(height - b[3], curY + step));
                    tse.saveConfig();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    selected.posSetter.accept(-1, curY);
                    tse.saveConfig();
                    return true;
                }
                case GLFW.GLFW_KEY_R -> {
                    selected.posSetter.accept(-1, selected.defaultY);
                    selected.scaleSetter.accept(selected.defaultScale);
                    tse.saveConfig();
                    return true;
                }
            }
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        tse.saveConfig();
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}