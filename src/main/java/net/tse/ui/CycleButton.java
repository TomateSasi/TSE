package net.tse.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class CycleButton extends AbstractWidget {

    private static final int ROW_H = 15;
    private static final int SEARCH_H = 16;
    private static final int MAX_VISIBLE = 8;

    private final List<String> options;
    private final List<Integer> filteredIndices = new ArrayList<>();
    private int index;
    private final Consumer<Integer> onChange;

    private boolean open = false;
    private boolean openUpward = false;
    private int scrollOffset = 0;
    private int hoverRow = -1;
    private String searchQuery = "";

    public CycleButton(int x, int y, int width, int height, List<String> options, int initialIndex,
                       Consumer<Integer> onChange) {
        super(x, y, width, height, Component.empty());
        this.options = options;
        this.index = options.isEmpty() ? 0 : Math.max(0, Math.min(options.size() - 1, initialIndex));
        this.onChange = onChange;
        rebuildFilter();
    }

    public int getIndex() { return index; }
    public void setIndex(int i) { if (!options.isEmpty()) this.index = Math.max(0, Math.min(options.size() - 1, i)); }
    public String current() { return options.isEmpty() ? "" : options.get(index); }
    public boolean isOpen() { return open; }

    public void close() {
        open = false;
        searchQuery = "";
        rebuildFilter();
    }

    private boolean showSearchBar() {
        return options.size() > 5 || !searchQuery.isEmpty();
    }

    private void rebuildFilter() {
        filteredIndices.clear();
        String q = searchQuery.toLowerCase().trim();
        for (int i = 0; i < options.size(); i++) {
            if (q.isEmpty() || options.get(i).toLowerCase().contains(q)) {
                filteredIndices.add(i);
            }
        }
        scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset));
    }

    private int visibleCount() { return Math.max(1, Math.min(MAX_VISIBLE, filteredIndices.size())); }
    private int headerOffset() { return showSearchBar() ? SEARCH_H + 2 : 0; }
    private int listHeight() { return headerOffset() + visibleCount() * ROW_H + 4; }
    private int maxScroll() { return Math.max(0, filteredIndices.size() - MAX_VISIBLE); }

    private int[] listBounds() {
        int x = getX(), y = getY(), w = Math.max(getWidth(), 130), h = getHeight();
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
        int rowsTop = b[1] + 2 + headerOffset();
        if (mouseY < rowsTop) return;
        int row = (int) ((mouseY - rowsTop) / ROW_H);
        int filterIdx = row + scrollOffset;
        if (filterIdx >= 0 && filterIdx < filteredIndices.size()) {
            index = filteredIndices.get(filterIdx);
            close();
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
        int rowsTop = b[1] + 2 + headerOffset();
        if (mouseY < rowsTop) { hoverRow = -1; return; }
        hoverRow = (int) ((mouseY - rowsTop) / ROW_H);
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        if (options.isEmpty()) return;
        if (open) {
            close();
        } else {
            open = true;
            searchQuery = "";
            rebuildFilter();
            var mc = Minecraft.getInstance();
            int screenH = mc.getWindow().getGuiScaledHeight();
            openUpward = (getY() + getHeight() + listHeight() + 4) > screenH;
            int posInFilter = filteredIndices.indexOf(index);
            scrollOffset = Math.max(0, Math.min(maxScroll(), Math.max(0, posInFilter) - MAX_VISIBLE / 2));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (!open) return false;
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (!searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0;
                rebuildFilter();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (!filteredIndices.isEmpty()) {
                int pick = (hoverRow >= 0 && hoverRow + scrollOffset < filteredIndices.size())
                        ? filteredIndices.get(hoverRow + scrollOffset)
                        : filteredIndices.get(0);
                index = pick;
                close();
                if (onChange != null) onChange.accept(index);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (!open) return false;
        char c = (char) input.codepoint();
        if (c >= 32 && c != 127 && searchQuery.length() < 28) {
            searchQuery += c;
            scrollOffset = 0;
            rebuildFilter();
            return true;
        }
        return false;
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
        MCTheme.fillRoundedCard(context, lx, ly, lw, lh, 4, MCTheme.PANEL_BG, MCTheme.accent(140));

        int curY = ly + 2;
        if (showSearchBar()) {
            MCTheme.fillRoundedCard(context, lx + 3, curY + 1, lw - 6, SEARCH_H - 2, 3, MCTheme.FIELD_BG, MCTheme.BORDER);
            String prompt = searchQuery.isEmpty() ? "Type to filter..." : (searchQuery + "_");
            int pCol = searchQuery.isEmpty() ? MCTheme.TEXT_DIM : MCTheme.accent(255);
            String trimmedPrompt = mc.font.plainSubstrByWidth(prompt, lw - 14);
            MCTheme.drawTextHD(context, mc.font, trimmedPrompt, lx + 7, curY + 4, pCol, false);
            curY += SEARCH_H + 2;
        }

        if (filteredIndices.isEmpty()) {
            MCTheme.drawTextHD(context, mc.font, "No matches", lx + 6, curY + 3, MCTheme.TEXT_DIM, false);
            return;
        }

        int shown = Math.min(MAX_VISIBLE, filteredIndices.size() - scrollOffset);
        for (int i = 0; i < shown; i++) {
            int optIdx = filteredIndices.get(i + scrollOffset);
            int rowY = curY + i * ROW_H;
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
            int trackH = shown * ROW_H - 4;
            int thumbH = Math.max(8, (int) (trackH * (shown / (float) filteredIndices.size())));
            int thumbY = curY + 2 + (int) ((trackH - thumbH) * (scrollOffset / (float) maxScroll()));
            context.fill(barX, curY + 2, barX + 1, curY + 2 + trackH, MCTheme.BORDER);
            context.fill(barX, thumbY, barX + 1, thumbY + thumbH, MCTheme.accent(200));
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.literal(current()));
    }
}