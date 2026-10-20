package net.tse.ui;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class CycleButton extends AbstractWidget {

    private static final int ROW_H = 15;
    private static final int MAX_VISIBLE = 8;

    private final List<String> options;
    private int index;
    private final Consumer<Integer> onChange;

    private boolean open = false;
    private boolean openUpward = false;
    private int scrollOffset = 0;
    private int hoverRow = -1;

    public CycleButton(int x, int y, int width, int height, List<String> options, int initialIndex,
                       Consumer<Integer> onChange) {
        super(x, y, width, height, Component.empty());
        this.options = options;
        this.index = options.isEmpty() ? 0 : Math.max(0, Math.min(options.size() - 1, initialIndex));
        this.onChange = onChange;
    }

    public int getIndex() { return index; }
    public void setIndex(int i) { if (!options.isEmpty()) this.index = Math.max(0, Math.min(options.size() - 1, i)); }
    public String current() { return options.isEmpty() ? "" : options.get(index); }
    public boolean isOpen() { return open; }
    public void close() { open = false; }

    private int visibleCount() { return Math.min(MAX_VISIBLE, options.size()); }
    private int listHeight() { return visibleCount() * ROW_H + 4; }
    private int maxScroll() { return Math.max(0, options.size() - MAX_VISIBLE); }

    private int[] listBounds() {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int listY = openUpward ? y - listHeight() - 2 : y + h + 2;
        return new int[]{x, listY, w, listHeight()};
    }

    public boolean isPointInList(double mouseX, double mouseY) {
        if (!open) return false;
        int[] b = listBounds();
        return mouseX >= b[0] && mouseX <= b[0] + b[2] && mouseY >= b[1] && mouseY <= b[1] + b[3];
    }

    public void selectAt(double mouseX, double mouseY) {
        int[] b = listBounds();
        int row = (int) ((mouseY - (b[1] + 2)) / ROW_H);
        int optIdx = row + scrollOffset;
        if (optIdx >= 0 && optIdx < options.size()) {
            index = optIdx;
            open = false;
            if (onChange != null) onChange.accept(index);
        }
    }

    public void scrollList(int amount) {
        if (!open) return;
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset - amount));
    }

    public void updateHover(double mouseX, double mouseY) {
        if (!open || !isPointInList(mouseX, mouseY)) { hoverRow = -1; return; }
        int[] b = listBounds();
        hoverRow = (int) ((mouseY - (b[1] + 2)) / ROW_H);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        if (options.isEmpty()) return;
        open = !open;
        if (open) {
            var mc = Minecraft.getInstance();
            int screenH = mc.getWindow().getGuiScaledHeight();
            openUpward = (getY() + getHeight() + listHeight() + 4) > screenH;
            scrollOffset = Math.max(0, Math.min(maxScroll(), index - MAX_VISIBLE / 2));
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        var mc = Minecraft.getInstance();
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int bg = isHovered() || open ? MCTheme.BUTTON_BG_HOVER : MCTheme.BUTTON_BG;
        int border = open ? MCTheme.accent(180) : (isHovered() ? MCTheme.accent(100) : MCTheme.CARD_BORDER);
        MCTheme.fillRoundedCard(context, x, y, w, h, 4, bg, border);

        String text = current();
        int maxTextW = w - 18;
        String trimmed = mc.font.plainSubstrByWidth(text, maxTextW);
        if (!trimmed.equals(text)) trimmed = trimmed + "..";
        MCTheme.drawTextHD(context, mc.font, trimmed, x + 6, y + (h - 8) / 2, MCTheme.TEXT, false);
        String arrow = open ? "▲" : "▼";
        int arrowColor = open ? MCTheme.accent(255) : MCTheme.TEXT_DIM;
        MCTheme.drawTextHD(context, mc.font, arrow, x + w - mc.font.width(arrow) - 5,
                y + (h - 8) / 2, arrowColor, false);
    }

    public void renderDropdown(GuiGraphicsExtractor context) {
        if (!open) return;
        var mc = Minecraft.getInstance();
        int[] b = listBounds();
        int lx = b[0], ly = b[1], lw = b[2], lh = b[3];
        MCTheme.fillRoundedCard(context, lx, ly, lw, lh, 4, MCTheme.PANEL_BG, MCTheme.accent(130));

        int shown = Math.min(visibleCount(), options.size() - scrollOffset);
        for (int i = 0; i < shown; i++) {
            int optIdx = i + scrollOffset;
            int rowY = ly + 2 + i * ROW_H;
            boolean hovered = i == hoverRow;
            boolean selected = optIdx == index;
            if (hovered) {
                MCTheme.fillRounded(context, lx + 2, rowY, lw - 4, ROW_H, 3, MCTheme.BUTTON_BG_HOVER);
            } else if (selected) {
                MCTheme.fillRounded(context, lx + 2, rowY, lw - 4, ROW_H, 3, MCTheme.accent(32));
            }
            int color = selected ? MCTheme.accent(255) : MCTheme.TEXT;
            String txt = mc.font.plainSubstrByWidth(options.get(optIdx), lw - 12);
            MCTheme.drawTextHD(context, mc.font, txt, lx + 6, rowY + (ROW_H - 8) / 2, color, false);
        }
        if (maxScroll() > 0) {
            int barX = lx + lw - 3;
            int trackH = lh - 6;
            int thumbH = Math.max(8, (int) (trackH * (visibleCount() / (float) options.size())));
            int thumbY = ly + 3 + (int) ((trackH - thumbH) * (scrollOffset / (float) maxScroll()));
            context.fill(barX, ly + 3, barX + 1, ly + 3 + trackH, MCTheme.BORDER);
            context.fill(barX, thumbY, barX + 1, thumbY + thumbH, MCTheme.accent(200));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.literal(current()));
    }
}