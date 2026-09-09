package net.tse.gui;

import com.example.ModConfig;
import com.example.tse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.tse.ui.CycleButton;
import net.tse.ui.FlatButton;
import net.tse.ui.FlatTextField;
import net.tse.ui.MCTheme;

import java.util.ArrayList;
import java.util.List;

public class ChatTriggerPopupScreen extends Screen {
    private final Screen parentChat;
    private final String rawMessage;
    private final int boxX, boxY;
    private int selectedCategory = 0;

    private CycleButton categoryDropdown;

    public ChatTriggerPopupScreen(Screen parentChat, String rawMessage, int mouseX, int mouseY) {
        super(Component.literal("Create Trigger"));
        this.parentChat = parentChat;
        this.rawMessage = rawMessage;

        int boxW = 220, boxH = 75;
        this.boxX = Math.min(mouseX, Minecraft.getInstance().getWindow().getGuiScaledWidth() - boxW - 5);
        this.boxY = Math.min(mouseY, Minecraft.getInstance().getWindow().getGuiScaledHeight() - boxH - 5);
    }

    @Override
    protected void init() {
        int boxW = 220;

        addRenderableWidget(FlatButton.builder(Component.literal("X"), b -> {
            Minecraft.getInstance().setScreen(parentChat);
        }).dimensions(boxX + boxW - 18, boxY + 6, 12, 12).build());

        FlatTextField keywordField = new FlatTextField(font, boxX + 10, boxY + 24, boxW - 20, 16, Component.literal("Keyword"));
        keywordField.setMaxLength(256);
        keywordField.setValue(rawMessage);
        addRenderableWidget(keywordField);

        List<String> catNames = new ArrayList<>();
        for (ModConfig.ChatCategory c : tse.config.chatCategories) catNames.add(c.name);
        if (catNames.isEmpty()) {
            catNames.add("General");
            tse.config.chatCategories.add(new ModConfig.ChatCategory("General"));
        }

        categoryDropdown = new CycleButton(boxX + 10, boxY + 46, 130, 16, catNames, 0, i -> selectedCategory = i);
        addRenderableWidget(categoryDropdown);

        addRenderableWidget(FlatButton.builder(Component.literal("Add"), b -> {
            String kw = keywordField.getValue().trim();
            if (!kw.isEmpty()) {
                ModConfig.ChatCategory cat = tse.config.chatCategories.get(selectedCategory);
                ModConfig.ChatRule newRule = new ModConfig.ChatRule();
                newRule.messageKeyword = kw;
                cat.rules.add(0, newRule);
                cat.collapsed = false;
                tse.saveConfig();
            }
            Minecraft.getInstance().setScreen(parentChat);
        }).dimensions(boxX + 146, boxY + 46, 64, 16).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parentChat != null) parentChat.extractRenderState(context, -1, -1, delta);

        int boxW = 220, boxH = 72;
        MCTheme.fillRounded(context, boxX, boxY, boxW, boxH, 6, MCTheme.PANEL_BG);
        context.outline(boxX, boxY, boxW, boxH, MCTheme.CARD_BORDER);
        MCTheme.drawTextHD(context, font, "New Chat Trigger", boxX + 10, boxY + 8, MCTheme.accent(255), false);

        super.extractRenderState(context, mouseX, mouseY, delta);

        if (categoryDropdown != null && categoryDropdown.isOpen()) {
            categoryDropdown.updateHover(mouseX, mouseY);
            categoryDropdown.renderDropdown(context);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x(), mouseY = click.y();

        if (categoryDropdown != null && categoryDropdown.isOpen()) {
            if (categoryDropdown.isPointInList(mouseX, mouseY)) {
                categoryDropdown.selectAt(mouseX, mouseY);
                return true;
            }
            categoryDropdown.close();
        }
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (categoryDropdown != null && categoryDropdown.isOpen() && categoryDropdown.isPointInList(mouseX, mouseY)) {
            categoryDropdown.scrollList((int) Math.signum(verticalAmount));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}