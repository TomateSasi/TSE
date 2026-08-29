package com.example;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public List<Category>     categories     = new ArrayList<>();
    public List<ChatCategory> chatCategories = new ArrayList<>();


    public List<ChatRule> chatRules = new ArrayList<>();

    public VoidgloomSettings  voidgloom     = new VoidgloomSettings();
    public boolean            masterEnabled = true;
    public String             theme         = "Default";



    public void migrateLegacyChatRules() {
        if (!chatRules.isEmpty()) {
            ChatCategory def = new ChatCategory("General");
            def.rules.addAll(chatRules);
            chatCategories.add(def);
            chatRules.clear();
        }
        if (chatCategories.isEmpty()) {
            chatCategories.add(new ChatCategory("General"));
        }
    }

    public static class VoidgloomSettings {
        public boolean enabled        = false;
        public String  reminderSound  = "Meow";
        public int     reminderVolume = 100;
        public boolean overlayEnabled = true;
        public String  overlayText    = "&cSNEAK!";
        public int     overlayColor   = 0xFFFF4444;
        public float   overlayScale   = 2.0f;
        public int     overlayX       = -1;
        public int     overlayY       = 80;
        public boolean overlayRainbow = false;
        public boolean overlayBlink   = true;
    }

    public static class Category {
        public String         name      = "New Category";
        public boolean        collapsed = false;
        public boolean        pinned    = false;
        public List<SoundRule> rules    = new ArrayList<>();

        public Category() {}
        public Category(String name) { this.name = name; }

        public Category copy() {
            Category c = new Category(this.name);
            c.collapsed = this.collapsed; c.pinned = this.pinned;
            for (SoundRule r : this.rules) c.rules.add(r.copy());
            return c;
        }
    }

    public static class ChatCategory {
        public String         name      = "New Category";
        public boolean        collapsed = false;
        public boolean        pinned    = false;
        public List<ChatRule> rules     = new ArrayList<>();

        public ChatCategory() {}
        public ChatCategory(String name) { this.name = name; }

        public ChatCategory copy() {
            ChatCategory c = new ChatCategory(this.name);
            c.collapsed = this.collapsed; c.pinned = this.pinned;
            for (ChatRule r : this.rules) c.rules.add(r.copy());
            return c;
        }
    }

    public static class SoundRule {
        public String  itemKeyword     = "";
        public String  ruleId          = "";
        public String  locationKeyword = "";
        public int     delaySeconds    = 0;
        public boolean enabled         = true;
        public boolean pinned          = false;
        public boolean collapsed       = false;
        public boolean sneakOnly       = false;
        public String  keybind         = "RIGHT_CLICK";
        public String  soundFile       = "Meow";
        public int     volume          = 100;

        public boolean overlayEnabled    = false;
        public String  overlayText       = "NOW!";
        public int     overlayColor      = 0xFFFFFF00;
        public float   overlayScale      = 2.0f;
        public int     overlayX          = -1;
        public int     overlayY          = 80;
        public int     overlayDurationMs = 2000;
        public boolean overlayRainbow    = false;

        public boolean closeWarnEnabled          = false;
        public String  closeWarnText             = "GET READY!";
        public int     closeWarnColor            = 0xFFFF4444;
        public float   closeWarnScale            = 1.5f;
        public int     closeWarnX                = -1;
        public int     closeWarnY                = 60;
        public int     closeWarnSecondsBeforeEnd = 3;
        public boolean closeWarnRainbow          = false;
        public boolean closeWarnBlink            = true;

        public transient boolean isOnCooldown       = false;
        public transient long    timerStartMs       = 0;
        public transient boolean timerActive        = false;
        public transient String  lastItemName       = "";
        public transient long    overlayShowUntilMs = 0;

        public SoundRule() {}
        public SoundRule(String item, String loc, int delay, boolean enabled) {
            this.itemKeyword = item; this.locationKeyword = loc;
            this.delaySeconds = delay; this.enabled = enabled;
        }

        public SoundRule copy() {
            SoundRule r = new SoundRule();
            r.itemKeyword = this.itemKeyword; r.ruleId = this.ruleId; r.locationKeyword = this.locationKeyword;
            r.delaySeconds = this.delaySeconds; r.enabled = this.enabled;
            r.pinned = this.pinned; r.sneakOnly = this.sneakOnly;
            r.keybind = this.keybind; r.soundFile = this.soundFile; r.volume = this.volume;
            r.collapsed = this.collapsed;
            r.overlayEnabled = this.overlayEnabled; r.overlayText = this.overlayText;
            r.overlayColor = this.overlayColor; r.overlayScale = this.overlayScale;
            r.overlayX = this.overlayX; r.overlayY = this.overlayY;
            r.overlayDurationMs = this.overlayDurationMs; r.overlayRainbow = this.overlayRainbow;
            r.closeWarnEnabled = this.closeWarnEnabled; r.closeWarnText = this.closeWarnText;
            r.closeWarnColor = this.closeWarnColor; r.closeWarnScale = this.closeWarnScale;
            r.closeWarnX = this.closeWarnX; r.closeWarnY = this.closeWarnY;
            r.closeWarnSecondsBeforeEnd = this.closeWarnSecondsBeforeEnd;
            r.closeWarnRainbow = this.closeWarnRainbow; r.closeWarnBlink = this.closeWarnBlink;
            return r;
        }
    }

    public static class ChatRule {
        public String  messageKeyword = "";
        public String  ruleId         = "";
        public String  soundFile      = "Meow";
        public int     volume         = 100;
        public boolean enabled        = true;
        public boolean pinned         = false;
        public boolean collapsed      = false;
        public boolean exactMatch     = false;
        public boolean systemOnly     = false;

        public boolean loopEnabled    = false;
        public String  loopEndTrigger = "";

        public ChatRule() {}
        public ChatRule copy() {
            ChatRule r = new ChatRule();
            r.messageKeyword = this.messageKeyword; r.ruleId = this.ruleId; r.soundFile = this.soundFile;
            r.volume = this.volume; r.enabled = this.enabled; r.pinned = this.pinned; r.collapsed = this.collapsed;
            r.exactMatch = this.exactMatch; r.systemOnly = this.systemOnly;
            r.loopEnabled = this.loopEnabled; r.loopEndTrigger = this.loopEndTrigger;
            return r;
        }
    }

    public ModConfig copy() {
        ModConfig c = new ModConfig();
        for (Category     cat : this.categories)     c.categories.add(cat.copy());
        for (ChatCategory cc  : this.chatCategories) c.chatCategories.add(cc.copy());
        VoidgloomSettings v = new VoidgloomSettings();
        v.enabled = this.voidgloom.enabled; v.reminderSound = this.voidgloom.reminderSound;
        v.reminderVolume = this.voidgloom.reminderVolume; v.overlayEnabled = this.voidgloom.overlayEnabled;
        v.overlayText = this.voidgloom.overlayText; v.overlayColor = this.voidgloom.overlayColor;
        v.overlayScale = this.voidgloom.overlayScale; v.overlayX = this.voidgloom.overlayX;
        v.overlayY = this.voidgloom.overlayY; v.overlayRainbow = this.voidgloom.overlayRainbow;
        v.overlayBlink = this.voidgloom.overlayBlink;
        c.voidgloom = v;
        return c;
    }
}