package net.tse;

import net.minecraft.client.Minecraft;
import net.tse.gui.TSEScreen;


public class TSEGui {

    public static final TSEGui INSTANCE = new TSEGui();
    private TSEGui() {}

    public void toggleVisible() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof TSEScreen) {
            client.setScreen(null);
        } else {
            client.setScreen(new TSEScreen());
        }
    }

    public boolean isVisible() {
        return Minecraft.getInstance().screen instanceof TSEScreen;
    }

    public void hide() {
        Minecraft client = Minecraft.getInstance();
        if (client.screen instanceof TSEScreen) client.setScreen(null);
    }
}
