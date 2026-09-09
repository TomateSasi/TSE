package com.example.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.tse.gui.ChatTriggerPopupScreen;
import org.joml.Matrix3x2f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onChatRightClick(MouseButtonEvent click, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().handle();

        boolean shiftDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (click.button() == 1 && shiftDown) {
            double mouseX = click.x();
            double mouseY = click.y();
            String textToCopy = "";

            try {
                Object chatGui = null;
                for (Method m : mc.gui.getClass().getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType().getSimpleName().toLowerCase().contains("chat")) {
                        chatGui = m.invoke(mc.gui);
                        break;
                    }
                }
                if (chatGui == null) {
                    for (Field f : mc.gui.getClass().getDeclaredFields()) {
                        if (f.getType().getSimpleName().toLowerCase().contains("chat")) {
                            f.setAccessible(true);
                            chatGui = f.get(mc.gui);
                            break;
                        }
                    }
                }

                if (chatGui != null) {
                    HoveredTextFinder finder = new HoveredTextFinder(mc.font, (int) mouseX, (int) mouseY);

                    Method captureMethod = null;
                    for (Method m : chatGui.getClass().getMethods()) {
                        if (m.getParameterCount() == 4 && m.getParameterTypes()[0] == ActiveTextCollector.class) {
                            captureMethod = m;
                            break;
                        }
                    }

                    if (captureMethod != null) {
                        int ticks = 0;
                        for (Method m : mc.gui.getClass().getMethods()) {
                            if (m.getParameterCount() == 0 && m.getReturnType() == int.class && m.getName().toLowerCase().contains("tick")) {
                                ticks = (Integer) m.invoke(mc.gui);
                                break;
                            }
                        }
                        if (ticks == 0) {
                            for (Field f : mc.gui.getClass().getDeclaredFields()) {
                                if (f.getType() == int.class && f.getName().toLowerCase().contains("tick")) {
                                    f.setAccessible(true);
                                    ticks = f.getInt(mc.gui);
                                    break;
                                }
                            }
                        }

                        captureMethod.invoke(chatGui, finder, mc.getWindow().getGuiScaledHeight(), ticks, ChatComponent.DisplayMode.FOREGROUND);

                        FormattedCharSequence hoveredText = finder.getHoveredText();

                        if (hoveredText != null) {
                            List<?> trimmedMessages = null;
                            for (Field f : chatGui.getClass().getDeclaredFields()) {
                                if (List.class.isAssignableFrom(f.getType())) {
                                    f.setAccessible(true);
                                    List<?> list = (List<?>) f.get(chatGui);
                                    if (list != null && !list.isEmpty() && list.get(0).getClass().getSimpleName().contains("Line")) {
                                        trimmedMessages = list;
                                        break;
                                    }
                                }
                            }

                            if (trimmedMessages != null) {
                                for (Object lineObj : trimmedMessages) {
                                    FormattedCharSequence content = null;
                                    for (Field f : lineObj.getClass().getDeclaredFields()) {
                                        if (f.getType() == FormattedCharSequence.class) {
                                            f.setAccessible(true);
                                            content = (FormattedCharSequence) f.get(lineObj);
                                            break;
                                        }
                                    }
                                    if (content == null) {
                                        for (Method m : lineObj.getClass().getDeclaredMethods()) {
                                            if (m.getReturnType() == FormattedCharSequence.class) {
                                                m.setAccessible(true);
                                                content = (FormattedCharSequence) m.invoke(lineObj);
                                                break;
                                            }
                                        }
                                    }

                                    if (content == hoveredText) {
                                        Object parent = null;
                                        for (Field f : lineObj.getClass().getDeclaredFields()) {
                                            if (f.getType().getSimpleName().equals("GuiMessage")) {
                                                f.setAccessible(true);
                                                parent = f.get(lineObj);
                                                break;
                                            }
                                        }
                                        if (parent == null) {
                                            for (Method m : lineObj.getClass().getDeclaredMethods()) {
                                                if (m.getReturnType().getSimpleName().equals("GuiMessage")) {
                                                    m.setAccessible(true);
                                                    parent = m.invoke(lineObj);
                                                    break;
                                                }
                                            }
                                        }

                                        if (parent != null) {
                                            for (Field f : parent.getClass().getDeclaredFields()) {
                                                if (Component.class.isAssignableFrom(f.getType())) {
                                                    f.setAccessible(true);
                                                    Component comp = (Component) f.get(parent);
                                                    textToCopy = comp.getString().trim();
                                                    break;
                                                }
                                            }
                                            if (textToCopy.isEmpty()) {
                                                for (Method m : parent.getClass().getDeclaredMethods()) {
                                                    if (Component.class.isAssignableFrom(m.getReturnType())) {
                                                        m.setAccessible(true);
                                                        Component comp = (Component) m.invoke(parent);
                                                        textToCopy = comp.getString().trim();
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            if (textToCopy.isEmpty() && !com.example.tse.recentMessages.isEmpty()) {
                textToCopy = com.example.tse.recentMessages.get(0);
            }

            if (!textToCopy.isEmpty()) {
                mc.setScreen(new ChatTriggerPopupScreen((ChatScreen)(Object)this, textToCopy, (int)mouseX, (int)mouseY));
                cir.setReturnValue(true);
            }
        }
    }

    private static class HoveredTextFinder implements ActiveTextCollector {
        private final Font font;
        private final int mouseX;
        private final int mouseY;
        private ActiveTextCollector.Parameters defaultParameters = new ActiveTextCollector.Parameters(new Matrix3x2f());
        private FormattedCharSequence hoveredText = null;

        public HoveredTextFinder(Font font, int mouseX, int mouseY) {
            this.font = font;
            this.mouseX = mouseX;
            this.mouseY = mouseY;
        }

        public FormattedCharSequence getHoveredText() {
            return hoveredText;
        }

        @Override
        public ActiveTextCollector.Parameters defaultParameters() {
            return defaultParameters;
        }

        @Override
        public void defaultParameters(ActiveTextCollector.Parameters newParameters) {
            defaultParameters = newParameters;
        }

        @Override
        public void accept(TextAlignment alignment, int anchorX, int y, ActiveTextCollector.Parameters parameters, FormattedCharSequence text) {
            int leftX = alignment.calculateLeft(anchorX, font, text);
            GuiTextRenderState renderState = new GuiTextRenderState(
                    font, text, parameters.pose(), leftX, y, -1, 0, true, true, parameters.scissor()
            );
            ActiveTextCollector.findElementUnderCursor(renderState, (float) mouseX, (float) mouseY, p -> {
                hoveredText = text;
            });
        }

        @Override
        public void acceptScrolling(Component message, int centerX, int left, int right, int top, int bottom, ActiveTextCollector.Parameters parameters) {
            ActiveTextCollector.super.defaultScrollingHelper(message, centerX, left, right, top, bottom, font.width(message), 9, parameters);
        }
    }
}