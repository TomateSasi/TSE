package net.tse.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;


public class FlatTextField extends EditBox {

    public FlatTextField(Font textRenderer, int x, int y, int width, int height, Component text) {
        super(textRenderer, x, y, width, height, text);
        addFormatter((s, firstCharIndex) -> MCTheme.styled(s).getVisualOrderText());
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        super.extractWidgetRenderState(context, mouseX, mouseY, delta);
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        int c = isFocused() ? MCTheme.FIELD_BG_HOVER : MCTheme.FIELD_BG;
        context.fill(x, y, x + w, y + 1, c);
        context.fill(x, y + h - 1, x + w, y + h, c);
        context.fill(x, y, x + 1, y + h, c);
        context.fill(x + w - 1, y, x + w, y + h, c);
    }
}