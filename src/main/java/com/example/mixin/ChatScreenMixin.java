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
                for (Field f : mc.gui.getClass().getDeclaredFields()) {
                    if (f.getType() == ChatComponent.class) {
                        f.setAccessible(true);
                        chatGui = f.get(mc.gui);
                        break;
                    }
                }
                if (chatGui == null) {
                    for (Method m : mc.gui.getClass().getMethods()) {
                        if (m.getReturnType() == ChatComponent.class && m.getParameterCount() == 0) {
                            m.setAccessible(true);
                            chatGui = m.invoke(mc.gui);
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
                            if (m.getReturnType() == int.class && m.getParameterCount() == 0 && m.getName().toLowerCase().contains("tick")) {
                                m.setAccessible(true);
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

                        captureMethod.setAccessible(true);
                        captureMethod.invoke(chatGui, finder, mc.getWindow().getGuiScaledHeight(), ticks, ChatComponent.DisplayMode.FOREGROUND);

                        FormattedCharSequence hoveredText = finder.getHoveredText();

                        if (hoveredText != null) {
                            List<?> trimmedMessages = null;
                            for (Field f : chatGui.getClass().getDeclaredFields()) {
                                if (List.class.isAssignableFrom(f.getType())) {
                                    f.setAccessible(true);
                                    List<?> list = (List<?>) f.get(chatGui);
                                    if (list != null && !list.isEmpty()) {
                                        Object first = list.get(0);
                                        boolean hasFcs = false;
                                        for (Field mf : first.getClass().getDeclaredFields()) {
                                            if (mf.getType() == FormattedCharSequence.class) {
                                                hasFcs = true;
                                                break;
                                            }
                                        }
                                        if (!hasFcs) {
                                            for (Method mm : first.getClass().getDeclaredMethods()) {
                                                if (mm.getReturnType() == FormattedCharSequence.class && mm.getParameterCount() == 0) {
                                                    hasFcs = true;
                                                    break;
                                                }
                                            }
                                        }
                                        if (hasFcs) {
                                            trimmedMessages = list;
                                            break;
                                        }
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
                                            if (m.getReturnType() == FormattedCharSequence.class && m.getParameterCount() == 0) {
                                                m.setAccessible(true);
                                                content = (FormattedCharSequence) m.invoke(lineObj);
                                                break;
                                            }
                                        }
                                    }

                                    if (content == hoveredText) {
                                        Object parentMsg = null;
                                        for (Field f : lineObj.getClass().getDeclaredFields()) {
                                            if (f.getType().getSimpleName().equals("GuiMessage")) {
                                                f.setAccessible(true);
                                                parentMsg = f.get(lineObj);
                                                break;
                                            }
                                        }
                                        if (parentMsg == null) {
                                            for (Method m : lineObj.getClass().getDeclaredMethods()) {
                                                if (m.getReturnType().getSimpleName().equals("GuiMessage") && m.getParameterCount() == 0) {
                                                    m.setAccessible(true);
                                                    parentMsg = m.invoke(lineObj);
                                                    break;
                                                }
                                            }
                                        }

                                        if (parentMsg != null) {
                                            for (Field f : parentMsg.getClass().getDeclaredFields()) {
                                                if (Component.class.isAssignableFrom(f.getType())) {
                                                    f.setAccessible(true);
                                                    Component comp = (Component) f.get(parentMsg);
                                                    textToCopy = comp.getString().trim();
                                                    break;
                                                }
                                            }
                                            if (textToCopy.isEmpty()) {
                                                for (Method m : parentMsg.getClass().getDeclaredMethods()) {
                                                    if (Component.class.isAssignableFrom(m.getReturnType()) && m.getParameterCount() == 0) {
                                                        m.setAccessible(true);
                                                        Component comp = (Component) m.invoke(parentMsg);
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
                mc.gui.setScreen(new ChatTriggerPopupScreen((ChatScreen)(Object)this, textToCopy, (int)mouseX, (int)mouseY));
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