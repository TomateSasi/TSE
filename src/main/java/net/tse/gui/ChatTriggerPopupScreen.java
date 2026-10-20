package net.tse.gui;

import com.example.ModConfig;
import com.example.tse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.tse.ui.CycleButton;
import net.tse.ui.FlatButton;
import net.tse.ui.FlatTextField;
import net.tse.ui.MCTheme;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ChatTriggerPopupScreen extends Screen {

    private static final int BOX_W = 236;
    private static final int BOX_H = 82;

    private final Screen parentChat;
    private final String rawMessage;
    private final int boxX, boxY;
    private int selectedCategory = 0;

    private FlatTextField keywordField;
    private CycleButton categoryDropdown;

    public ChatTriggerPopupScreen(Screen parentChat, String rawMessage, int mouseX, int mouseY) {
        super(Component.literal("Create Trigger"));
        this.parentChat = parentChat;
        this.rawMessage = rawMessage;

        var win = Minecraft.getInstance().getWindow();
        this.boxX = Math.max(6, Math.min(mouseX, win.getGuiScaledWidth() - BOX_W - 6));
        this.boxY = Math.max(6, Math.min(mouseY, win.getGuiScaledHeight() - BOX_H - 6));
    }

    @Override
    protected void init() {
        clearWidgets();

        addRenderableWidget(FlatButton.builder(Component.literal("✕"), b -> onClose())
                .dimensions(boxX + BOX_W - 22, boxY + 5, 16, 16)
                .style(FlatButton.Style.GHOST)
                .build());

        keywordField = new FlatTextField(font, boxX + 10, boxY + 28, BOX_W - 20, 18, Component.literal("Keyword"));
        keywordField.setMaxLength(256);
        keywordField.setHint(MCTheme.styled("Trigger keyword..."));
        keywordField.setValue(rawMessage);
        addRenderableWidget(keywordField);
        setInitialFocus(keywordField);

        tse.config.migrateLegacyChatRules();
        List<String> catNames = new ArrayList<>();
        for (ModConfig.ChatCategory c : tse.config.chatCategories) catNames.add(c.name);
        if (catNames.isEmpty()) {
            catNames.add("General");
            tse.config.chatCategories.add(new ModConfig.ChatCategory("General"));
        }

        categoryDropdown = new CycleButton(boxX + 10, boxY + 52, 142, 18, catNames, selectedCategory, i -> selectedCategory = i);
        addRenderableWidget(categoryDropdown);

        addRenderableWidget(FlatButton.builder(Component.literal("+ Add"), b -> submitTrigger())
                .dimensions(boxX + 158, boxY + 52, 68, 18)
                .style(FlatButton.Style.ACCENT)
                .build());
    }

    private void submitTrigger() {
        String kw = keywordField.getValue().trim();
        if (!kw.isEmpty()) {
            tse.pushUndo();
            ModConfig.ChatCategory cat = tse.config.chatCategories.get(selectedCategory);
            ModConfig.ChatRule newRule = new ModConfig.ChatRule();
            newRule.messageKeyword = kw;
            cat.rules.add(0, newRule);
            cat.collapsed = false;
            tse.saveConfig();
        }
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (parentChat != null) parentChat.extractRenderState(context, -1, -1, delta);

        MCTheme.fillRoundedCard(context, boxX, boxY, BOX_W, BOX_H, 8, MCTheme.WINDOW_BG, MCTheme.CARD_BORDER);
        MCTheme.fillRounded(context, boxX, boxY + 6, 2, 12, 1, MCTheme.accent(230));
        MCTheme.drawTextHD(context, font, "New Chat Trigger", boxX + 10, boxY + 9, MCTheme.TEXT, false);
        context.fill(boxX + 8, boxY + 22, boxX + BOX_W - 8, boxY + 23, MCTheme.BORDER);

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
            if (!categoryDropdown.isMouseOver(mouseX, mouseY)) {
                categoryDropdown.close();
            }
        }

        if (super.mouseClicked(click, doubleClick)) {
            return true;
        }

        if (mouseX < boxX || mouseX > boxX + BOX_W || mouseY < boxY || mouseY > boxY + BOX_H) {
            onClose();
            return true;
        }
        return false;
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
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            submitTrigger();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parentChat);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}