package net.tse.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class FlatTextField extends EditBox {

    private final int boxX, boxY, boxW, boxH;

    public FlatTextField(Font textRenderer, int x, int y, int width, int height, Component text) {
        super(textRenderer, x + 5, y + (height - 8) / 2, Math.max(8, width - 10), height, text);
        this.boxX = x;
        this.boxY = y;
        this.boxW = width;
        this.boxH = height;
        setBordered(false);
        addFormatter((s, firstCharIndex) -> MCTheme.styled(s).getVisualOrderText());
    }

    @Override
    public void setX(int x) {
        super.setX(x + 5);
    }

    @Override
    public void setY(int y) {
        super.setY(y + (boxH - 8) / 2);
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(Math.max(8, width - 10));
    }

    private int outerX() { return super.getX() - 5; }
    private int outerY() { return super.getY() - (boxH - 8) / 2; }
    private int outerW() { return super.getWidth() + 10; }
    private int outerH() { return boxH; }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        int x = outerX(), y = outerY(), w = outerW(), h = outerH();
        return visible && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = outerX(), y = outerY(), w = outerW(), h = outerH();
        boolean hovered = isMouseOver(mouseX, mouseY);
        int bg = isFocused() || hovered ? MCTheme.FIELD_BG_HOVER : MCTheme.FIELD_BG;
        int border = isFocused() ? MCTheme.accent(180) : (hovered ? MCTheme.CARD_BORDER : MCTheme.BORDER);
        MCTheme.fillRoundedCard(context, x, y, w, h, 4, bg, border);
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
    }
}