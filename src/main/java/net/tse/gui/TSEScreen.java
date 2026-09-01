package net.tse.gui;

import net.tse.ui.FlatTextField;
import net.tse.ui.FlatButton;
import com.example.ModConfig;
import com.example.ProfileManager;
import com.example.tse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.tse.ui.ColorPickerScreen;
import net.tse.ui.CycleButton;
import net.tse.ui.IconButton;
import net.tse.ui.MCTheme;
import net.tse.ui.PreciseSlider;
import net.tse.ui.ToggleWidget;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TSEScreen extends Screen {

    private static final String[] PAGES = {
            "Sound Rules", "Chat Triggers", "Voidgloom Seraph", "General", "Profiles"
    };
    private static final String[] SB_LOCATIONS = {
            "Any", "Private Island", "Hub", "The Farming Islands", "The Park",
            "Galatea", "Torrhus Canyon", "Spider's Den", "The End", "Crimson Isle",
            "Kuudra", "Gold Mine", "Deep Caverns", "Dwarven Mines", "Crystal Hollows",
            "Backwater Bayou", "Lotus Atoll", "Dungeon Hub", "The Catacombs", "The Rift",
            "Garden", "Jerry's Workshop", "Dark Auction"
    };
    private static final String[] SB_LOCATION_KEYS = {
            "", "dynamic", "hub", "farming_1", "foraging_1", "foraging_2",
            "foraging_3", "combat_1", "combat_3", "crimson_isle", "kuudra", "mining_1",
            "mining_2", "mining_3", "crystal_hollows", "fishing_1", "lotus_atoll",
            "dungeon_hub", "dungeon", "rift", "garden", "winter_island", "dark_auction"
    };
    private static final String DISCORD_URL = "https://discord.gg/R3xPtspRtE";
    private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("tse", "textures/gui/logo.png");
    private static final int LOGO_TEX_W = 1024, LOGO_TEX_H = 380;
    private static final int LOGO_W = 64, LOGO_H = (int) (LOGO_W * (LOGO_TEX_H / (float) LOGO_TEX_W));

    private static final Identifier DISCORD_TEXTURE = Identifier.fromNamespaceAndPath("tse", "textures/gui/discord.png");
    private static final int DISCORD_TEX_W = 128, DISCORD_TEX_H = 128;

    private int activePage = 0;
    private int scrollOffset = 0;
    private int contentTotalHeight = 0;

    private int winX, winY, winW, winH;
    private int sideW = 122;

    private static final int SLIDER_KNOB_MARGIN = 8;
    private int contentX, contentY, contentW, contentH;

    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private AbstractWidget focusedContent = null;

    private final EditBox ruleSearch;
    private final EditBox chatSearch;
    private final EditBox newProfileName;

    private ModConfig.SoundRule capturingRule = null;

    public TSEScreen() {
        super(Component.literal("TSE"));
        var mc = Minecraft.getInstance();
        ruleSearch = new FlatTextField(mc.font, 0, 0, 180, 18, Component.literal("Search"));
        ruleSearch.setHint(MCTheme.styled("Search..."));
        ruleSearch.setResponder(s -> {
            rebuildContent();
            focusedContent = ruleSearch;
            ruleSearch.setFocused(true);
        });
        chatSearch = new FlatTextField(mc.font, 0, 0, 180, 18, Component.literal("Search"));
        chatSearch.setHint(MCTheme.styled("Search..."));
        chatSearch.setResponder(s -> {
            rebuildContent();
            focusedContent = chatSearch;
            chatSearch.setFocused(true);
        });
        newProfileName = new FlatTextField(mc.font, 0, 0, 200, 18, Component.literal("Profile name"));
        newProfileName.setHint(MCTheme.styled("New profile name..."));
    }

    @Override
    protected void init() {
        winW = Math.min(width - 20, 660);
        winH = Math.min(height - 20, 420);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        contentX = winX + sideW + 14;
        contentY = winY + 48;
        contentW = winX + winW - contentX - 6;
        contentH = winY + winH - contentY - 6;

        clearWidgets();

        for (int i = 0; i < PAGES.length; i++) {
            int idx = i;
            String label = PAGES[i];
            addRenderableWidget(FlatButton.builder(Component.literal(label), b -> switchPage(idx))
                    .dimensions(winX + 8, winY + 50 + i * 20, sideW - 14, 18).build());
        }

        addRenderableWidget(new IconButton(winX + winW - 30, winY + 6, 22, DISCORD_TEXTURE, DISCORD_TEX_W, DISCORD_TEX_H,
                () -> net.minecraft.util.Util.getPlatform().openUri(DISCORD_URL)));

        rebuildContent();
    }

    private void switchPage(int idx) {
        activePage = idx;
        scrollOffset = 0;
        rebuildContent();
    }

    private void rebuildContent() {
        buildPageOnce();
        int maxScroll = Math.max(0, contentTotalHeight - contentH);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
            buildPageOnce();
        }
    }

    private void buildPageOnce() {
        contentWidgets.clear();
        focusedContent = null;
        sectionLabels.clear();
        inlineLabels.clear();
        profileLabels.clear();
        cursorY = contentY - scrollOffset;

        switch (PAGES[activePage]) {
            case "Sound Rules" -> pageSoundRules();
            case "Chat Triggers" -> pageChatTriggers();
            case "Voidgloom Seraph" -> pageVoidgloom();
            case "General" -> pageGeneral();
            case "Profiles" -> pageProfiles();
        }

        contentTotalHeight = (cursorY + scrollOffset) - contentY;
    }

    private int cursorY;

    private <T extends AbstractWidget> T add(T w) {
        contentWidgets.add(w);
        return w;
    }

    private void pageSoundRules() {
        cursorY += 4;
        int left = contentX;

        ruleSearch.setX(left); ruleSearch.setY(cursorY); ruleSearch.setWidth(100);
        add(ruleSearch);
        add(FlatButton.builder(Component.literal("+ Category"), b -> {
                    tse.pushUndo();
                    tse.config.categories.add(new ModConfig.Category("New Category"));
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(left + 116, cursorY, 62, 18).build());
        add(FlatButton.builder(Component.literal("Refresh"), b -> { tse.refreshAvailableSounds(); rebuildContent(); })
                .dimensions(left + 182, cursorY, 50, 18).build());
        add(FlatButton.builder(Component.literal("Undo"), b -> { tse.undo(); rebuildContent(); })
                .dimensions(left + 236, cursorY, 36, 18).build());
        cursorY += 22;

        String q = ruleSearch.getValue().toLowerCase().trim();

        if (tse.config.categories.isEmpty()) {
            cursorY += 20;
            return;
        }

        List<ModConfig.Category> displayCats = new ArrayList<>(tse.config.categories);
        displayCats.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));

        for (ModConfig.Category cat : displayCats) {
            boolean catHit = q.isEmpty() || cat.name.toLowerCase().contains(q);
            boolean anyHit = catHit;
            if (!anyHit) for (var r : cat.rules) if (ruleHit(r, q)) { anyHit = true; break; }
            if (!anyHit) continue;

            drawCategoryHeader(cat);

            if (!cat.collapsed) {
                List<ModConfig.SoundRule> displayRules = new ArrayList<>(cat.rules);
                displayRules.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));
                for (ModConfig.SoundRule rule : displayRules) {
                    if (!catHit && !ruleHit(rule, q)) continue;
                    drawRuleCard(cat, rule);
                }
            }
            cursorY += 6;
        }
    }

    private void drawCategoryHeader(ModConfig.Category cat) {
        int left = contentX;
        int w = contentW;
        int h = 26;

        add(FlatButton.builder(Component.literal(cat.collapsed ? "▶" : "▼"), b -> {
                    cat.collapsed = !cat.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(left, cursorY, 20, h).build());

        EditBox name = new FlatTextField(font, left + 24, cursorY + 3, 110, 18, Component.literal("Name"));
        name.setValue(cat.name);
        name.setResponder(s -> { cat.name = s; tse.saveConfig(); });
        add(name);

        add(new ToggleWidget(left + 142, cursorY + 5, cat.pinned, v -> { cat.pinned = v; tse.saveConfig(); rebuildContent(); }));

        add(FlatButton.builder(Component.literal("+ Rule"), b -> {
                    tse.pushUndo();
                    cat.rules.add(new ModConfig.SoundRule("item", "location", 0, true));
                    cat.collapsed = false;
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(left + w - 150, cursorY, 70, h).build());

        add(deleteButton(left + w - 74, cursorY, 74, h, () -> {
            tse.pushUndo(); tse.config.categories.remove(cat); tse.saveConfig(); rebuildContent();
        }));

        cursorY += h + 4;
    }

    private void drawRuleCard(ModConfig.Category cat, ModConfig.SoundRule rule) {
        int left = contentX + 14;
        int w = contentW - 14;
        int rowH = 18;

        add(FlatButton.builder(Component.literal(rule.collapsed ? "▶" : "▼"), b -> {
                    rule.collapsed = !rule.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(contentX, cursorY, 12, 18).build());
        add(new ToggleWidget(left, cursorY + 3, rule.enabled, v -> { rule.enabled = v; tse.saveConfig(); }));
        int x = left + ToggleWidget.W + 6;
        EditBox item = new FlatTextField(font, x, cursorY, 88, 18, Component.literal("Item"));
        item.setValue(rule.itemKeyword);
        item.setHint(MCTheme.styled("Item..."));
        item.setResponder(s -> { rule.itemKeyword = s; tse.saveConfig(); });
        add(item);
        x += 96;

        int locIdx = 0;
        for (int i = 0; i < SB_LOCATION_KEYS.length; i++)
            if (SB_LOCATION_KEYS[i].equalsIgnoreCase(rule.locationKeyword)) { locIdx = i; break; }
        add(new CycleButton(x, cursorY, 120, 18, Arrays.asList(SB_LOCATIONS), locIdx, i -> {
            rule.locationKeyword = i == 0 ? "" : SB_LOCATION_KEYS[i]; tse.saveConfig();
        }));
        add(deleteButton(left + w - 44, cursorY, 44, 18, () -> {
            tse.pushUndo(); cat.rules.remove(rule); tse.saveConfig(); rebuildContent();
        }));

        if (rule.collapsed) {
            cursorY += rowH + 8;
            return;
        }
        cursorY += rowH + 12;

        x = left;
        add(new PreciseSlider(x, cursorY, 130, rule.delaySeconds, 0, 1000, 1,
                v -> String.format("%.0fs", v), v -> { rule.delaySeconds = (int) v; tse.saveConfig(); },
                "Delay before playing"));
        x += 146;
        x = toggleWithLabel(x, cursorY + 3, rule.sneakOnly, "Sneak only", v -> { rule.sneakOnly = v; tse.saveConfig(); });
        toggleWithLabel(x, cursorY + 3, rule.pinned, "Pinned", v -> { rule.pinned = v; tse.saveConfig(); rebuildContent(); });
        cursorY += rowH + 4;

        x = left;
        boolean listening = capturingRule == rule;
        String kbLabel = listening ? "< press a key/click >" : ("[ " + fmtKey(rule.keybind) + " ]");
        add(FlatButton.builder(Component.literal(kbLabel), b -> {
                    capturingRule = listening ? null : rule;
                    rebuildContent();
                })
                .dimensions(x, cursorY, 120, 18).build());
        x += 124;

        List<String> sounds = tse.availableSounds.isEmpty() ? List.of("Meow") : tse.availableSounds;
        int sIdx = Math.max(0, sounds.indexOf(rule.soundFile));
        add(new CycleButton(x, cursorY, 120, 18, sounds, sIdx, i -> { rule.soundFile = sounds.get(i); tse.saveConfig(); }));
        x += 124;

        add(FlatButton.builder(Component.literal("Play"), b -> tse.playSound(rule.soundFile, rule.volume))
                .dimensions(x, cursorY, 36, 18).build());
        cursorY += rowH + 12;

        x = left;
        add(new PreciseSlider(x, cursorY, 130, rule.volume, 0, 200, 1,
                v -> String.format("%.0f%%", v), v -> { rule.volume = (int) v; tse.saveConfig(); },
                "Volume"));
        x += 140;
        add(FlatButton.builder(Component.literal("Overlay Settings..."), b ->
                        Minecraft.getInstance().gui.setScreen(new RuleDetailScreen(this, rule)))
                .dimensions(x, cursorY, w - (x - left), 18).build());

        cursorY += rowH + 10;
    }

    private void pageChatTriggers() {
        tse.config.migrateLegacyChatRules();
        cursorY += 4;
        int left = contentX;

        chatSearch.setX(left); chatSearch.setY(cursorY); chatSearch.setWidth(100);
        add(chatSearch);
        add(FlatButton.builder(Component.literal("+ Category"), b -> {
                    tse.pushUndo();
                    tse.config.chatCategories.add(new ModConfig.ChatCategory("New Category"));
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(left + 116, cursorY, 62, 18).build());
        add(FlatButton.builder(Component.literal("Refresh"), b -> { tse.refreshAvailableSounds(); rebuildContent(); })
                .dimensions(left + 182, cursorY, 50, 18).build());
        add(FlatButton.builder(Component.literal("Undo"), b -> { tse.undo(); rebuildContent(); })
                .dimensions(left + 236, cursorY, 36, 18).build());
        cursorY += 22;

        String q = chatSearch.getValue().toLowerCase().trim();

        if (tse.config.chatCategories.isEmpty()) { cursorY += 20; return; }

        List<ModConfig.ChatCategory> displayCats = new ArrayList<>(tse.config.chatCategories);
        displayCats.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));

        for (ModConfig.ChatCategory cat : displayCats) {
            boolean catHit = q.isEmpty() || cat.name.toLowerCase().contains(q);
            boolean anyHit = catHit;
            if (!anyHit) for (var r : cat.rules)
                if (r.messageKeyword.toLowerCase().contains(q) || r.soundFile.toLowerCase().contains(q)) { anyHit = true; break; }
            if (!anyHit) continue;

            drawChatCategoryHeader(cat);

            if (!cat.collapsed) {
                List<ModConfig.ChatRule> displayRules = new ArrayList<>(cat.rules);
                displayRules.sort((a, b) -> Boolean.compare(b.pinned, a.pinned));
                for (ModConfig.ChatRule rule : displayRules) {
                    if (!catHit && !rule.messageKeyword.toLowerCase().contains(q)
                            && !rule.soundFile.toLowerCase().contains(q)) continue;
                    drawChatRuleCard(cat, rule);
                }
            }
            cursorY += 6;
        }
    }

    private void drawChatCategoryHeader(ModConfig.ChatCategory cat) {
        int left = contentX;
        int w = contentW;
        int h = 26;

        add(FlatButton.builder(Component.literal(cat.collapsed ? "▶" : "▼"), b -> {
                    cat.collapsed = !cat.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(left, cursorY, 20, h).build());

        EditBox name = new FlatTextField(font, left + 24, cursorY + 3, 110, 18, Component.literal("Name"));
        name.setValue(cat.name);
        name.setResponder(s -> { cat.name = s; tse.saveConfig(); });
        add(name);

        add(new ToggleWidget(left + 142, cursorY + 5, cat.pinned, v -> { cat.pinned = v; tse.saveConfig(); rebuildContent(); }));

        add(FlatButton.builder(Component.literal("+ Trigger"), b -> {
                    tse.pushUndo();
                    cat.rules.add(new ModConfig.ChatRule());
                    cat.collapsed = false;
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(left + w - 150, cursorY, 76, h).build());

        add(deleteButton(left + w - 68, cursorY, 68, h, () -> {
            tse.pushUndo(); tse.config.chatCategories.remove(cat); tse.saveConfig(); rebuildContent();
        }));

        cursorY += h + 4;
    }

    private void drawChatRuleCard(ModConfig.ChatCategory cat, ModConfig.ChatRule rule) {
        int left = contentX + 14;
        int rowH = 18;

        add(FlatButton.builder(Component.literal(rule.collapsed ? "▶" : "▼"), b -> {
                    rule.collapsed = !rule.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(contentX, cursorY, 12, 18).build());
        add(new ToggleWidget(left, cursorY + 3, rule.enabled, v -> { rule.enabled = v; tse.saveConfig(); }));
        int x = left + ToggleWidget.W + 6;
        EditBox kw = new FlatTextField(font, x, cursorY, 128, 18, Component.literal("Keyword"));
        kw.setValue(rule.messageKeyword);
        kw.setHint(MCTheme.styled("Keyword..."));
        kw.setResponder(s -> { rule.messageKeyword = s; tse.saveConfig(); });
        add(kw);

        add(deleteButton(left + contentW - 14 - 44, cursorY, 44, 18, () -> {
            tse.pushUndo(); cat.rules.remove(rule); tse.saveConfig(); rebuildContent();
        }));

        if (rule.collapsed) {
            cursorY += rowH + 8;
            return;
        }
        cursorY += rowH + 4;

        x = left;
        x = toggleWithLabel(x, cursorY + 3, rule.exactMatch, "Exact", v -> { rule.exactMatch = v; tse.saveConfig(); });
        x = toggleWithLabel(x, cursorY + 3, rule.systemOnly, "System only", v -> { rule.systemOnly = v; tse.saveConfig(); });
        toggleWithLabel(x, cursorY + 3, rule.pinned, "Pinned", v -> { rule.pinned = v; tse.saveConfig(); rebuildContent(); });
        cursorY += rowH + 4;

        x = left;
        List<String> sounds = tse.availableSounds.isEmpty() ? List.of("Meow") : tse.availableSounds;
        int sIdx = Math.max(0, sounds.indexOf(rule.soundFile));
        add(new CycleButton(x, cursorY, 120, 18, sounds, sIdx, i -> { rule.soundFile = sounds.get(i); tse.saveConfig(); }));
        x += 124;
        add(FlatButton.builder(Component.literal("Play"), b -> tse.playSound(rule.soundFile, rule.volume))
                .dimensions(x, cursorY, 36, 18).build());
        cursorY += rowH + 12;

        x = left;
        add(new PreciseSlider(x, cursorY, 110, rule.volume, 0, 200, 1,
                v -> String.format("%.0f%%", v), v -> { rule.volume = (int) v; tse.saveConfig(); },
                "Volume"));
        x += 126;
        int afterLoop = toggleWithLabel(x, cursorY + 3, rule.loopEnabled, "Loop", v -> {
            rule.loopEnabled = v;
            if (!v) tse.stopChatLoop(rule);
            tse.saveConfig();
            rebuildContent();
        });
        if (rule.loopEnabled) {
            EditBox end = new FlatTextField(font, afterLoop, cursorY, contentW - 14 - (afterLoop - left), 18,
                    Component.literal("Ending trigger"));
            end.setHint(MCTheme.styled("Ending trigger..."));
            end.setValue(rule.loopEndTrigger);
            end.setResponder(s -> { rule.loopEndTrigger = s; tse.saveConfig(); });
            add(end);
        }

        cursorY += rowH + 10;
    }

    private void pageVoidgloom() {
        ModConfig.VoidgloomSettings v = tse.config.voidgloom;
        int left = contentX;
        cursorY += 4;

        add(new ToggleWidget(left, cursorY, v.enabled, val -> { v.enabled = val; tse.saveConfig(); }));
        addLabel(left + 40, cursorY + 4, v.enabled ? "Enabled" : "Disabled");
        cursorY += 30;

        addSection("Reminder Sound");
        List<String> sounds = tse.availableSounds.isEmpty() ? List.of("Meow") : tse.availableSounds;
        int sIdx = Math.max(0, sounds.indexOf(v.reminderSound));
        add(new CycleButton(left, cursorY, 120, 18, sounds, sIdx, i -> { v.reminderSound = sounds.get(i); tse.saveConfig(); }));
        add(FlatButton.builder(Component.literal("Play"), b -> tse.playSound(v.reminderSound, v.reminderVolume))
                .dimensions(left + 124, cursorY, 36, 18).build());
        cursorY += 22 + 12;
        add(new PreciseSlider(left, cursorY, 130, v.reminderVolume, 0, 200, 1,
                s -> String.format("%.0f%%", s), s -> { v.reminderVolume = (int) s; tse.saveConfig(); },
                "Reminder volume"));
        cursorY += 24;

        addSection("Overlay (shown while sneaking is required)");
        add(new ToggleWidget(left, cursorY, v.overlayEnabled, val -> { v.overlayEnabled = val; tse.saveConfig(); }));
        addLabel(left + 40, cursorY + 4, "Enabled");
        cursorY += 24;
        EditBox text = new FlatTextField(font, left, cursorY, 240, 18, Component.literal("Overlay text"));
        text.setValue(v.overlayText);
        text.setResponder(s -> { v.overlayText = s; tse.saveConfig(); });
        add(text);
        cursorY += 24;

        add(new ToggleWidget(left, cursorY, v.overlayRainbow, val -> { v.overlayRainbow = val; tse.saveConfig(); }));
        addLabel(left + 40, cursorY + 4, "Rainbow");
        add(new ToggleWidget(left + 120, cursorY, v.overlayBlink, val -> { v.overlayBlink = val; tse.saveConfig(); }));
        addLabel(left + 160, cursorY + 4, "Blink");
        add(FlatButton.builder(Component.literal("Color"), b ->
                        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(this, v.overlayColor,
                                c -> { v.overlayColor = c; tse.saveConfig(); })))
                .dimensions(left + 230, cursorY - 1, 66, 18).build());
        cursorY += 24 + 12;

        add(new PreciseSlider(left, cursorY, 130, v.overlayScale, 0.5, 8.0, 0.1,
                s -> String.format("%.1fx", s), s -> { v.overlayScale = (float) s; tse.saveConfig(); },
                "Overlay text size"));
        cursorY += 24;

        boolean centered = v.overlayX < 0;
        add(new ToggleWidget(left, cursorY, centered, val -> { v.overlayX = val ? -1 : 0; tse.saveConfig(); rebuildContent(); }));
        addLabel(left + 40, cursorY + 4, "Centered");
        if (!centered) {
            EditBox xf = new FlatTextField(font, left + 130, cursorY - 1, 50, 18, Component.literal("X"));
            xf.setValue(String.valueOf(v.overlayX));
            xf.setResponder(s -> {
                if (!s.isEmpty() && !s.matches("-?\\d*")) { xf.setValue(s.replaceAll("[^-\\d]", "")); return; }
                try { v.overlayX = Integer.parseInt(s); tse.saveConfig(); } catch (Exception ignored) {}
            });
            add(xf);
        }
        EditBox yf = new FlatTextField(font, left + 190, cursorY - 1, 50, 18, Component.literal("Y"));
        yf.setValue(String.valueOf(v.overlayY));
        yf.setResponder(s -> {
            if (!s.isEmpty() && !s.matches("-?\\d*")) { yf.setValue(s.replaceAll("[^-\\d]", "")); return; }
            try { v.overlayY = Integer.parseInt(s); tse.saveConfig(); } catch (Exception ignored) {}
        });
        add(yf);
        cursorY += 26;
    }

    private void pageGeneral() {
        int left = contentX;
        cursorY += 4;

        addSection("Master Toggle");
        add(new ToggleWidget(left, cursorY, tse.config.masterEnabled, v -> { tse.config.masterEnabled = v; tse.saveConfig(); rebuildContent(); }));
        addLabel(left + 40, cursorY + 4, tse.config.masterEnabled ? "Mod ENABLED" : "Mod DISABLED");
        cursorY += 30;

        addSection("Theme");
        String[] names = MCTheme.THEME_NAMES;
        int curIdx = 0;
        for (int i = 0; i < names.length; i++) if (names[i].equals(tse.config.theme)) { curIdx = i; break; }
        add(new CycleButton(left, cursorY, 160, 18, Arrays.asList(names), curIdx, i -> {
            tse.config.theme = names[i];
            MCTheme.applyTheme(tse.config.theme);
            tse.saveConfig();
        }));
        cursorY += 28;

        addSection("Utilities");
        add(FlatButton.builder(Component.literal("Undo"), b -> { tse.undo(); rebuildContent(); })
                .dimensions(left, cursorY, 70, 18).build());
        add(FlatButton.builder(Component.literal("Save Config"), b -> tse.saveConfig())
                .dimensions(left + 76, cursorY, 100, 18).build());
        add(FlatButton.builder(Component.literal("Refresh Sounds"), b -> { tse.refreshAvailableSounds(); rebuildContent(); })
                .dimensions(left + 182, cursorY, 120, 18).build());
        add(FlatButton.builder(Component.literal("Open Sounds Folder"), b -> {
                    try { new ProcessBuilder("explorer.exe", tse.SOUNDS_DIR.getAbsolutePath()).start(); }
                    catch (Exception ignored) {}
                })
                .dimensions(left + 308, cursorY, 150, 18).build());
        cursorY += 30;

        addSection("Developer");
        add(FlatButton.builder(Component.literal("Run Audio Debug"), b -> tse.runAudioDebug())
                .dimensions(left, cursorY, 140, 18).build());
        cursorY += 30;
    }

    private void pageProfiles() {
        int left = contentX;
        cursorY += 4;

        newProfileName.setX(left); newProfileName.setY(cursorY); newProfileName.setWidth(200);
        add(newProfileName);
        add(FlatButton.builder(Component.literal("Save Current as Profile"), b -> {
                    ProfileManager.INSTANCE.createProfile(newProfileName.getValue(), tse.config);
                    newProfileName.setValue("");
                    rebuildContent();
                })
                .dimensions(left + 210, cursorY, 170, 18).build());
        cursorY += 30;

        if (ProfileManager.INSTANCE.profiles.isEmpty()) { cursorY += 20; return; }

        int cardH = 26;
        for (int i = 0; i < ProfileManager.INSTANCE.profiles.size(); i++) {
            ProfileManager.Profile profile = ProfileManager.INSTANCE.profiles.get(i);

            add(FlatButton.builder(Component.literal("Load"), b -> {
                        ProfileManager.INSTANCE.applyProfile(profile, tse.config);
                        rebuildContent();
                    })
                    .dimensions(left + contentW - 130, cursorY, 56, 18).build());
            add(deleteButton(left + contentW - 70, cursorY, 70, 18, () -> {
                ProfileManager.INSTANCE.deleteProfile(profile);
                rebuildContent();
            }));
            profileLabels.add(new String[]{profile.name, String.valueOf(cursorY)});
            cursorY += cardH;
        }
    }

    private final List<String[]> profileLabels = new ArrayList<>();

    private AbstractWidget deleteButton(int x, int y, int w, int h, Runnable action) {
        return FlatButton.builder(Component.literal("Delete"), b -> action.run())
                .dimensions(x, y, w, h).build();
    }

    private void addSection(String title) {
        cursorY += 10;
        sectionLabels.add(new Object[]{title, cursorY});
        cursorY += 20;
    }

    private void addLabel(int x, int y, String text) {
        inlineLabels.add(new Object[]{text, x, y});
    }

    private int toggleWithLabel(int x, int y, boolean value, String label, java.util.function.Consumer<Boolean> onChange) {
        add(new ToggleWidget(x, y, value, onChange));
        addLabel(x + ToggleWidget.W + 4, y + 2, label);
        int labelW = font.width(label);
        return x + ToggleWidget.W + 4 + labelW + 12;
    }

    private final List<Object[]> sectionLabels = new ArrayList<>();
    private final List<Object[]> inlineLabels = new ArrayList<>();

    private boolean ruleHit(ModConfig.SoundRule r, String q) {
        if (q.isEmpty()) return true;
        return r.itemKeyword.toLowerCase().contains(q)
                || r.locationKeyword.toLowerCase().contains(q)
                || r.soundFile.toLowerCase().contains(q)
                || r.keybind.toLowerCase().contains(q);
    }

    private String fmtKey(String kb) {
        return switch (kb) {
            case "LEFT_CLICK" -> "Left Click";
            case "RIGHT_CLICK" -> "Right Click";
            case "MIDDLE_CLICK" -> "Middle Click";
            default -> {
                if (!kb.startsWith("KEY_")) yield kb;
                try {
                    int k = Integer.parseInt(kb.substring(4));
                    yield switch (k) {
                        case 32 -> "Space"; case 257 -> "Enter"; case 258 -> "Tab";
                        case 259 -> "Back"; case 340 -> "LShift"; case 341 -> "LCtrl";
                        case 342 -> "LAlt"; case 344 -> "RShift"; case 345 -> "RCtrl";
                        case 346 -> "RAlt"; case 262 -> "Right"; case 263 -> "Left";
                        case 264 -> "Down"; case 265 -> "Up";
                        default -> {
                            if (k >= 65 && k <= 90) yield String.valueOf((char) k);
                            if (k >= 48 && k <= 57) yield String.valueOf((char) k);
                            if (k >= 290 && k <= 301) yield "F" + (k - 289);
                            if (k >= 320 && k <= 329) yield "Num" + (k - 320);
                            yield "Key" + k;
                        }
                    };
                } catch (Exception e) { yield kb; }
            }
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x88000000);
        MCTheme.fillRounded(context, winX, winY, winW, winH, 14, MCTheme.WINDOW_BG);

        context.blit(RenderPipelines.GUI_TEXTURED, LOGO_TEXTURE,
                winX + 8, winY + 4, 0f, 0f, LOGO_W, LOGO_H, LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
        context.fill(winX, winY + 34, winX + winW, winY + 35, MCTheme.BORDER);
        context.fill(winX + sideW, winY + 36, winX + sideW + 1, winY + winH - 6, MCTheme.BORDER);

        context.enableScissor(contentX - SLIDER_KNOB_MARGIN, contentY, contentX + contentW, contentY + contentH);

        for (Object[] s : sectionLabels) {
            String title = (String) s[0];
            int y = (int) s[1];
            MCTheme.drawTextHD(context, font, title, contentX, y, MCTheme.accent(255), false);
            context.fill(contentX, y + 10, contentX + contentW, y + 11, MCTheme.BORDER);
        }
        for (Object[] l : inlineLabels) {
            MCTheme.drawTextHD(context, font, (String) l[0], (int) l[1], (int) l[2], MCTheme.TEXT_DIM, false);
        }
        for (String[] p : profileLabels) {
            int y = Integer.parseInt(p[1]);
            MCTheme.drawTextHD(context, font, p[0], contentX, y + 5, MCTheme.TEXT, false);
        }

        for (AbstractWidget w : contentWidgets) {
            w.extractRenderState(context, mouseX, mouseY, delta);
        }
        context.disableScissor();

        super.extractRenderState(context, mouseX, mouseY, delta);

        for (AbstractWidget w : contentWidgets) {
            if (w instanceof CycleButton cb && cb.isOpen()) {
                cb.updateHover(mouseX, mouseY);
                cb.renderDropdown(context);
            }
        }

        if (capturingRule != null) {
            context.centeredText(font, "Press a key or click to bind...",
                    winX + winW / 2, winY + winH - 16, 0xFFFFAA33);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {

    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick) {
        double mouseX = click.x(), mouseY = click.y();
        int button = click.button();
        if (capturingRule != null) {
            if (button >= 0 && button <= 2) {
                tse.pushUndo();
                capturingRule.keybind = new String[]{"LEFT_CLICK", "RIGHT_CLICK", "MIDDLE_CLICK"}[button];
                tse.saveConfig();
            }
            capturingRule = null;
            rebuildContent();
            return true;
        }

        for (AbstractWidget w : contentWidgets) {
            if (w instanceof CycleButton cb && cb.isOpen() && cb.isPointInList(mouseX, mouseY)) {
                cb.selectAt(mouseX, mouseY);
                return true;
            }
        }

        for (AbstractWidget w : contentWidgets) {
            if (w instanceof CycleButton cb && cb.isOpen() && !w.isMouseOver(mouseX, mouseY)) {
                cb.close();
            }
        }

        if (mouseX >= contentX && mouseX <= contentX + contentW
                && mouseY >= contentY && mouseY <= contentY + contentH) {
            for (AbstractWidget w : contentWidgets) {
                if (w.isMouseOver(mouseX, mouseY)) {
                    if (focusedContent != null && focusedContent != w) focusedContent.setFocused(false);
                    focusedContent = w;
                    w.setFocused(true);
                    return w.mouseClicked(click, doubleClick);
                }
            }
        }
        if (focusedContent != null) focusedContent.setFocused(false);
        focusedContent = null;
        return super.mouseClicked(click, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double deltaX, double deltaY) {
        if (focusedContent != null) return focusedContent.mouseDragged(click, deltaX, deltaY);
        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (focusedContent != null) {
            boolean r = focusedContent.mouseReleased(click);
            return r;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        for (AbstractWidget w : contentWidgets) {
            if (w instanceof CycleButton cb && cb.isOpen() && cb.isPointInList(mouseX, mouseY)) {
                cb.scrollList((int) Math.signum(verticalAmount));
                return true;
            }
        }
        if (mouseX >= contentX && mouseX <= contentX + contentW
                && mouseY >= contentY && mouseY <= contentY + contentH) {
            int maxScroll = Math.max(0, contentTotalHeight - contentH);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int) (verticalAmount * 18)));
            rebuildContent();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int keyCode = input.key();
        if (capturingRule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                capturingRule = null;
                rebuildContent();
                return true;
            }
            tse.pushUndo();
            capturingRule.keybind = "KEY_" + keyCode;
            tse.saveConfig();
            capturingRule = null;
            rebuildContent();
            return true;
        }
        if (focusedContent != null) {
            boolean handled = focusedContent.keyPressed(input);
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                if (focusedContent instanceof FlatTextField) {
                    focusedContent.setFocused(false);
                    focusedContent = null;
                }
                return true;
            }
            if (handled) return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent input) {
        if (focusedContent != null && focusedContent.charTyped(input)) return true;
        return super.charTyped(input);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
