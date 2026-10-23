package net.tse.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class FlatTextField extends EditBox {

    private final int boxH;
    private float focusAnim = 0f;

    public FlatTextField(Font textRenderer, int x, int y, int width, int height, Component text) {
        super(textRenderer, x + 5, y + (height - 8) / 2, Math.max(8, width - 10), height, text);
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

    public void offsetY(int dy) {
        super.setY(super.getY() + dy);
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
        float target = isFocused() ? 1f : (hovered ? 0.45f : 0f);
        focusAnim += (target - focusAnim) * 0.25f;

        int bg = MCTheme.lerpColor(MCTheme.FIELD_BG, MCTheme.FIELD_BG_HOVER, Math.min(1f, focusAnim * 1.4f));
        int border = MCTheme.lerpColor(MCTheme.BORDER, MCTheme.accent(190), focusAnim);
        MCTheme.fillRoundedCard(context, x, y, w, h, 4, bg, border);
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
    }
}