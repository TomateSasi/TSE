package net.tse;

import net.minecraft.client.Minecraft;
import net.tse.gui.TSEScreen;

public class TSEGui {

    public static final TSEGui INSTANCE = new TSEGui();
    private TSEGui() {}

    public void toggleVisible() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof TSEScreen) {
            client.gui.setScreen(null);
        } else {
            client.gui.setScreen(new TSEScreen());
        }
    }

    public boolean isVisible() {
        return Minecraft.getInstance().gui.screen() instanceof TSEScreen;
    }

    public void hide() {
        Minecraft client = Minecraft.getInstance();
        if (client.gui.screen() instanceof TSEScreen) client.gui.setScreen(null);
    }
}
