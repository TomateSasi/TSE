package net.tse.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


public class IconButton extends AbstractWidget {

    public interface PressAction { void onPress(); }

    private final Identifier texture;
    private final int texW, texH;
    private final PressAction onPress;

    public IconButton(int x, int y, int size, Identifier texture, int texW, int texH, PressAction onPress) {
        super(x, y, size, size, Component.empty());
        this.texture = texture;
        this.texW = texW;
        this.texH = texH;
        this.onPress = onPress;
    }

    @Override
    public void onClick(MouseButtonEvent click, boolean doubleClick) {
        if (onPress != null) onPress.onPress();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        if (isHovered()) MCTheme.fillRounded(context, x - 3, y - 3, w + 6, h + 6, 4, MCTheme.BUTTON_BG_HOVER);
        context.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0f, 0f, w, h, texW, texH, texW, texH);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(net.minecraft.client.gui.narration.NarratedElementType.TITLE, Component.literal("Discord"));
    }
}