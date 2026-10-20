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
    private final int sideW = 126;

    private static final int SLIDER_KNOB_MARGIN = 8;
    private int contentX, contentY, contentW, contentH;

    private final List<AbstractWidget> contentWidgets = new ArrayList<>();
    private final List<FlatButton> navButtons = new ArrayList<>();
    private AbstractWidget focusedContent = null;

    private record CardRect(int x, int y, int w, int h, int fill, int border, boolean accentLeft) {}
    private final List<CardRect> cardRects = new ArrayList<>();

    private final EditBox ruleSearch;
    private final EditBox chatSearch;
    private final EditBox newProfileName;

    private ModConfig.SoundRule capturingRule = null;

    public TSEScreen() {
        super(Component.literal("TSE"));
        var mc = Minecraft.getInstance();
        ruleSearch = new FlatTextField(mc.font, 0, 0, 160, 18, Component.literal("Search"));
        ruleSearch.setHint(MCTheme.styled("Search rules..."));
        ruleSearch.setResponder(s -> {
            rebuildContent();
            focusedContent = ruleSearch;
            ruleSearch.setFocused(true);
        });
        chatSearch = new FlatTextField(mc.font, 0, 0, 160, 18, Component.literal("Search"));
        chatSearch.setHint(MCTheme.styled("Search triggers..."));
        chatSearch.setResponder(s -> {
            rebuildContent();
            focusedContent = chatSearch;
            chatSearch.setFocused(true);
        });
        newProfileName = new FlatTextField(mc.font, 0, 0, 210, 18, Component.literal("Profile name"));
        newProfileName.setHint(MCTheme.styled("New profile name..."));
    }

    @Override
    protected void init() {
        winW = Math.min(width - 20, 670);
        winH = Math.min(height - 20, 426);
        winX = (width - winW) / 2;
        winY = (height - winH) / 2;

        contentX = winX + sideW + 12;
        contentY = winY + 42;
        contentW = winX + winW - contentX - 10;
        contentH = winY + winH - contentY - 8;

        clearWidgets();
        navButtons.clear();

        for (int i = 0; i < PAGES.length; i++) {
            int idx = i;
            String label = PAGES[i];
            FlatButton tab = FlatButton.builder(Component.literal(label), b -> switchPage(idx))
                    .dimensions(winX + 8, winY + 44 + i * 24, sideW - 16, 20)
                    .style(FlatButton.Style.TAB)
                    .selected(i == activePage)
                    .build();
            navButtons.add(tab);
            addRenderableWidget(tab);
        }

        addRenderableWidget(new IconButton(winX + winW - 30, winY + 7, 20, DISCORD_TEXTURE, DISCORD_TEX_W, DISCORD_TEX_H,
                () -> net.minecraft.util.Util.getPlatform().openUri(DISCORD_URL)));

        rebuildContent();
    }

    private void switchPage(int idx) {
        activePage = idx;
        scrollOffset = 0;
        for (int i = 0; i < navButtons.size(); i++) {
            navButtons.get(i).setSelected(i == activePage);
        }
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
        cardRects.clear();
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

    private void addCard(int x, int y, int w, int h, int fill, int border, boolean accentLeft) {
        cardRects.add(new CardRect(x, y, w, h, fill, border, accentLeft));
    }

    private void pageSoundRules() {
        cursorY += 2;
        int left = contentX;

        ruleSearch.setX(left);
        ruleSearch.setY(cursorY);
        ruleSearch.setWidth(150);
        add(ruleSearch);

        int btnRight = left + contentW;
        add(FlatButton.builder(Component.literal("Undo"), b -> { tse.undo(); rebuildContent(); })
                .dimensions(btnRight - 44, cursorY, 44, 18).build());
        add(FlatButton.builder(Component.literal("Refresh"), b -> { tse.refreshAvailableSounds(); rebuildContent(); })
                .dimensions(btnRight - 102, cursorY, 54, 18).build());
        add(FlatButton.builder(Component.literal("+ Category"), b -> {
                    tse.pushUndo();
                    tse.config.categories.add(new ModConfig.Category("New Category"));
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(btnRight - 178, cursorY, 72, 18)
                .style(FlatButton.Style.ACCENT)
                .build());
        cursorY += 26;

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

        addCard(left, cursorY, w, h, MCTheme.CARD_HEADER_FILL, MCTheme.CARD_BORDER, cat.pinned);

        add(FlatButton.builder(Component.literal(cat.collapsed ? "▶" : "▼"), b -> {
                    cat.collapsed = !cat.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(left + 4, cursorY + 4, 18, 18)
                .style(FlatButton.Style.GHOST)
                .build());

        EditBox name = new FlatTextField(font, left + 26, cursorY + 4, 130, 18, Component.literal("Name"));
        name.setValue(cat.name);
        name.setResponder(s -> { cat.name = s; tse.saveConfig(); });
        add(name);

        toggleWithLabel(left + 164, cursorY + 7, cat.pinned, "Pin", v -> {
            cat.pinned = v; tse.saveConfig(); rebuildContent();
        });

        add(FlatButton.builder(Component.literal("+ Rule"), b -> {
                    tse.pushUndo();
                    cat.rules.add(new ModConfig.SoundRule("item", "", 0, true));
                    cat.collapsed = false;
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(left + w - 122, cursorY + 4, 56, 18)
                .style(FlatButton.Style.ACCENT)
                .build());

        add(deleteButton(left + w - 62, cursorY + 4, 56, 18, () -> {
            tse.pushUndo(); tse.config.categories.remove(cat); tse.saveConfig(); rebuildContent();
        }));

        cursorY += h + 4;
    }

    private void drawRuleCard(ModConfig.Category cat, ModConfig.SoundRule rule) {
        int cardX = contentX + 8;
        int cardW = contentW - 8;
        int cardH = rule.collapsed ? 26 : 108;

        addCard(cardX, cursorY, cardW, cardH, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, rule.enabled);

        int left = cardX + 8;
        int innerW = cardW - 16;
        int rowY = cursorY + 4;

        add(FlatButton.builder(Component.literal(rule.collapsed ? "▶" : "▼"), b -> {
                    rule.collapsed = !rule.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(left, rowY, 16, 18)
                .style(FlatButton.Style.GHOST)
                .build());

        add(new ToggleWidget(left + 20, rowY + 3, rule.enabled, v -> { rule.enabled = v; tse.saveConfig(); }));

        int x = left + 20 + ToggleWidget.W + 6;
        EditBox item = new FlatTextField(font, x, rowY, 110, 18, Component.literal("Item"));
        item.setValue(rule.itemKeyword);
        item.setHint(MCTheme.styled("Item name..."));
        item.setResponder(s -> { rule.itemKeyword = s; tse.saveConfig(); });
        add(item);
        x += 116;

        int locIdx = 0;
        for (int i = 0; i < SB_LOCATION_KEYS.length; i++)
            if (SB_LOCATION_KEYS[i].equalsIgnoreCase(rule.locationKeyword)) { locIdx = i; break; }
        add(new CycleButton(x, rowY, 128, 18, Arrays.asList(SB_LOCATIONS), locIdx, i -> {
            rule.locationKeyword = i == 0 ? "" : SB_LOCATION_KEYS[i]; tse.saveConfig();
        }));

        add(deleteButton(left + innerW - 50, rowY, 50, 18, () -> {
            tse.pushUndo(); cat.rules.remove(rule); tse.saveConfig(); rebuildContent();
        }));

        if (rule.collapsed) {
            cursorY += cardH + 4;
            return;
        }

        rowY += 28;
        x = left;
        add(new PreciseSlider(x, rowY, 150, rule.delaySeconds, 0, 1000, 1,
                v -> String.format("%.0fs", v), v -> { rule.delaySeconds = (int) v; tse.saveConfig(); },
                "Cooldown / Delay"));
        x += 164;
        x = toggleWithLabel(x, rowY + 2, rule.sneakOnly, "Sneak only", v -> { rule.sneakOnly = v; tse.saveConfig(); });
        toggleWithLabel(x, rowY + 2, rule.pinned, "Pinned", v -> { rule.pinned = v; tse.saveConfig(); rebuildContent(); });

        rowY += 24;
        x = left;
        boolean listening = capturingRule == rule;
        String kbLabel = listening ? "Press key / click..." : ("Bind: " + fmtKey(rule.keybind));
        add(FlatButton.builder(Component.literal(kbLabel), b -> {
                    capturingRule = listening ? null : rule;
                    rebuildContent();
                })
                .dimensions(x, rowY, 124, 18)
                .style(listening ? FlatButton.Style.ACCENT : FlatButton.Style.DEFAULT)
                .build());
        x += 130;

        List<String> sounds = tse.availableSounds.isEmpty() ? List.of("Meow") : tse.availableSounds;
        int sIdx = Math.max(0, sounds.indexOf(rule.soundFile));
        add(new CycleButton(x, rowY, 134, 18, sounds, sIdx, i -> { rule.soundFile = sounds.get(i); tse.saveConfig(); }));
        x += 140;

        add(FlatButton.builder(Component.literal("Play"), b -> tse.playSound(rule.soundFile, rule.volume))
                .dimensions(x, rowY, 44, 18).build());

        rowY += 28;
        x = left;
        add(new PreciseSlider(x, rowY, 150, rule.volume, 0, 200, 1,
                v -> String.format("%.0f%%", v), v -> { rule.volume = (int) v; tse.saveConfig(); },
                "Volume"));
        x += 164;
        add(FlatButton.builder(Component.literal("Overlay & Warning Settings..."), b ->
                        Minecraft.getInstance().gui.setScreen(new RuleDetailScreen(this, rule)))
                .dimensions(x, rowY, innerW - (x - left), 18).build());

        cursorY += cardH + 5;
    }

    private void pageChatTriggers() {
        tse.config.migrateLegacyChatRules();
        cursorY += 2;
        int left = contentX;

        chatSearch.setX(left);
        chatSearch.setY(cursorY);
        chatSearch.setWidth(150);
        add(chatSearch);

        int btnRight = left + contentW;
        add(FlatButton.builder(Component.literal("Undo"), b -> { tse.undo(); rebuildContent(); })
                .dimensions(btnRight - 44, cursorY, 44, 18).build());
        add(FlatButton.builder(Component.literal("Refresh"), b -> { tse.refreshAvailableSounds(); rebuildContent(); })
                .dimensions(btnRight - 102, cursorY, 54, 18).build());
        add(FlatButton.builder(Component.literal("+ Category"), b -> {
                    tse.pushUndo();
                    tse.config.chatCategories.add(new ModConfig.ChatCategory("New Category"));
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(btnRight - 178, cursorY, 72, 18)
                .style(FlatButton.Style.ACCENT)
                .build());
        cursorY += 26;

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

        addCard(left, cursorY, w, h, MCTheme.CARD_HEADER_FILL, MCTheme.CARD_BORDER, cat.pinned);

        add(FlatButton.builder(Component.literal(cat.collapsed ? "▶" : "▼"), b -> {
                    cat.collapsed = !cat.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(left + 4, cursorY + 4, 18, 18)
                .style(FlatButton.Style.GHOST)
                .build());

        EditBox name = new FlatTextField(font, left + 26, cursorY + 4, 130, 18, Component.literal("Name"));
        name.setValue(cat.name);
        name.setResponder(s -> { cat.name = s; tse.saveConfig(); });
        add(name);

        toggleWithLabel(left + 164, cursorY + 7, cat.pinned, "Pin", v -> {
            cat.pinned = v; tse.saveConfig(); rebuildContent();
        });

        add(FlatButton.builder(Component.literal("+ Trigger"), b -> {
                    tse.pushUndo();
                    cat.rules.add(new ModConfig.ChatRule());
                    cat.collapsed = false;
                    tse.saveConfig();
                    rebuildContent();
                })
                .dimensions(left + w - 128, cursorY + 4, 62, 18)
                .style(FlatButton.Style.ACCENT)
                .build());

        add(deleteButton(left + w - 62, cursorY + 4, 56, 18, () -> {
            tse.pushUndo(); tse.config.chatCategories.remove(cat); tse.saveConfig(); rebuildContent();
        }));

        cursorY += h + 4;
    }

    private void drawChatRuleCard(ModConfig.ChatCategory cat, ModConfig.ChatRule rule) {
        int cardX = contentX + 8;
        int cardW = contentW - 8;
        int cardH = rule.collapsed ? 26 : 102;

        addCard(cardX, cursorY, cardW, cardH, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, rule.enabled);

        int left = cardX + 8;
        int innerW = cardW - 16;
        int rowY = cursorY + 4;

        add(FlatButton.builder(Component.literal(rule.collapsed ? "▶" : "▼"), b -> {
                    rule.collapsed = !rule.collapsed; tse.saveConfig(); rebuildContent();
                })
                .dimensions(left, rowY, 16, 18)
                .style(FlatButton.Style.GHOST)
                .build());

        add(new ToggleWidget(left + 20, rowY + 3, rule.enabled, v -> { rule.enabled = v; tse.saveConfig(); }));
        int x = left + 20 + ToggleWidget.W + 6;
        EditBox kw = new FlatTextField(font, x, rowY, 180, 18, Component.literal("Keyword"));
        kw.setValue(rule.messageKeyword);
        kw.setHint(MCTheme.styled("Chat keyword..."));
        kw.setResponder(s -> { rule.messageKeyword = s; tse.saveConfig(); });
        add(kw);

        add(deleteButton(left + innerW - 50, rowY, 50, 18, () -> {
            tse.pushUndo(); cat.rules.remove(rule); tse.saveConfig(); rebuildContent();
        }));

        if (rule.collapsed) {
            cursorY += cardH + 4;
            return;
        }

        rowY += 22;
        x = left;
        x = toggleWithLabel(x, rowY + 2, rule.exactMatch, "Exact match", v -> { rule.exactMatch = v; tse.saveConfig(); });
        x = toggleWithLabel(x, rowY + 2, rule.systemOnly, "System only", v -> { rule.systemOnly = v; tse.saveConfig(); });
        toggleWithLabel(x, rowY + 2, rule.pinned, "Pinned", v -> { rule.pinned = v; tse.saveConfig(); rebuildContent(); });

        rowY += 22;
        x = left;
        List<String> sounds = tse.availableSounds.isEmpty() ? List.of("Meow") : tse.availableSounds;
        int sIdx = Math.max(0, sounds.indexOf(rule.soundFile));
        add(new CycleButton(x, rowY, 140, 18, sounds, sIdx, i -> { rule.soundFile = sounds.get(i); tse.saveConfig(); }));
        x += 146;
        add(FlatButton.builder(Component.literal("Play"), b -> tse.playSound(rule.soundFile, rule.volume))
                .dimensions(x, rowY, 44, 18).build());

        rowY += 28;
        x = left;
        add(new PreciseSlider(x, rowY, 140, rule.volume, 0, 200, 1,
                v -> String.format("%.0f%%", v), v -> { rule.volume = (int) v; tse.saveConfig(); },
                "Volume"));
        x += 154;
        int afterLoop = toggleWithLabel(x, rowY + 2, rule.loopEnabled, "Loop", v -> {
            rule.loopEnabled = v;
            if (!v) tse.stopChatLoop(rule);
            tse.saveConfig();
            rebuildContent();
        });
        if (rule.loopEnabled) {
            EditBox end = new FlatTextField(font, afterLoop, rowY - 1, innerW - (afterLoop - left), 18,
                    Component.literal("Ending trigger"));
            end.setHint(MCTheme.styled("Stop keyword..."));
            end.setValue(rule.loopEndTrigger);
            end.setResponder(s -> { rule.loopEndTrigger = s; tse.saveConfig(); });
            add(end);
        }

        cursorY += cardH + 5;
    }

    private void pageVoidgloom() {
        ModConfig.VoidgloomSettings v = tse.config.voidgloom;
        int left = contentX + 10;
        cursorY += 4;

        addCard(contentX, cursorY, contentW, 28, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, v.enabled);
        add(new ToggleWidget(left, cursorY + 8, v.enabled, val -> { v.enabled = val; tse.saveConfig(); rebuildContent(); }));
        addLabel(left + 32, cursorY + 10, v.enabled ? "Voidgloom Boss Reminder Enabled" : "Voidgloom Boss Reminder Disabled");
        add(FlatButton.builder(Component.literal("Move HUD..."), b ->
                        Minecraft.getInstance().gui.setScreen(new TSEHudEditorScreen(this, null)))
                .dimensions(contentX + contentW - 96, cursorY + 5, 88, 18)
                .style(FlatButton.Style.ACCENT)
                .build());
        cursorY += 36;

        addSection("Reminder Sound");
        int soundCardY = cursorY;
        addCard(contentX, soundCardY, contentW, 64, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, false);
        cursorY += 8;
        List<String> sounds = tse.availableSounds.isEmpty() ? List.of("Meow") : tse.availableSounds;
        int sIdx = Math.max(0, sounds.indexOf(v.reminderSound));
        add(new CycleButton(left, cursorY, 140, 18, sounds, sIdx, i -> { v.reminderSound = sounds.get(i); tse.saveConfig(); }));
        add(FlatButton.builder(Component.literal("Play"), b -> tse.playSound(v.reminderSound, v.reminderVolume))
                .dimensions(left + 146, cursorY, 44, 18).build());
        cursorY += 30;
        add(new PreciseSlider(left, cursorY, 160, v.reminderVolume, 0, 200, 1,
                s -> String.format("%.0f%%", s), s -> { v.reminderVolume = (int) s; tse.saveConfig(); },
                "Reminder Volume"));
        cursorY = soundCardY + 72;

        addSection("Sneak Overlay");
        int ovCardY = cursorY;
        addCard(contentX, ovCardY, contentW, 134, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, false);
        cursorY += 8;
        toggleWithLabel(left, cursorY + 2, v.overlayEnabled, "Show Overlay", val -> { v.overlayEnabled = val; tse.saveConfig(); });
        cursorY += 22;

        EditBox text = new FlatTextField(font, left, cursorY, 240, 18, Component.literal("Overlay text"));
        text.setValue(v.overlayText);
        text.setResponder(s -> { v.overlayText = s; tse.saveConfig(); });
        add(text);
        cursorY += 24;

        int nx = toggleWithLabel(left, cursorY + 2, v.overlayRainbow, "Rainbow", val -> { v.overlayRainbow = val; tse.saveConfig(); });
        nx = toggleWithLabel(nx, cursorY + 2, v.overlayBlink, "Blink", val -> { v.overlayBlink = val; tse.saveConfig(); });
        add(FlatButton.builder(Component.literal("Edit Color"), b ->
                        Minecraft.getInstance().gui.setScreen(new ColorPickerScreen(this, v.overlayColor,
                                c -> { v.overlayColor = c; tse.saveConfig(); })))
                .dimensions(nx + 4, cursorY - 1, 74, 18).build());
        cursorY += 30;

        add(new PreciseSlider(left, cursorY, 160, v.overlayScale, 0.5, 8.0, 0.1,
                s -> String.format("%.1fx", s), s -> { v.overlayScale = (float) s; tse.saveConfig(); },
                "Overlay Text Scale"));

        boolean centered = v.overlayX < 0;
        int posX = left + 180;
        toggleWithLabel(posX, cursorY + 2, centered, "Centered", val -> {
            v.overlayX = val ? -1 : 0; tse.saveConfig(); rebuildContent();
        });
        int fx = posX + 82;
        if (!centered) {
            EditBox xf = new FlatTextField(font, fx, cursorY - 1, 48, 18, Component.literal("X"));
            xf.setValue(String.valueOf(v.overlayX));
            xf.setResponder(s -> {
                if (!s.isEmpty() && !s.matches("-?\\d*")) { xf.setValue(s.replaceAll("[^-\\d]", "")); return; }
                try { v.overlayX = Integer.parseInt(s); tse.saveConfig(); } catch (Exception ignored) {}
            });
            add(xf);
            fx += 54;
        }
        EditBox yf = new FlatTextField(font, fx, cursorY - 1, 48, 18, Component.literal("Y"));
        yf.setValue(String.valueOf(v.overlayY));
        yf.setResponder(s -> {
            if (!s.isEmpty() && !s.matches("-?\\d*")) { yf.setValue(s.replaceAll("[^-\\d]", "")); return; }
            try { v.overlayY = Integer.parseInt(s); tse.saveConfig(); } catch (Exception ignored) {}
        });
        add(yf);
        cursorY = ovCardY + 142;
    }

    private void pageGeneral() {
        int left = contentX + 10;
        cursorY += 4;

        addSection("Master Toggle");
        addCard(contentX, cursorY, contentW, 28, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, tse.config.masterEnabled);
        add(new ToggleWidget(left, cursorY + 8, tse.config.masterEnabled, v -> {
            tse.config.masterEnabled = v; tse.saveConfig(); rebuildContent();
        }));
        addLabel(left + 32, cursorY + 10, tse.config.masterEnabled ? "Mod is ENABLED" : "Mod is DISABLED");
        cursorY += 34;

        addSection("Theme & HUD");
        addCard(contentX, cursorY, contentW, 34, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, false);
        String[] names = MCTheme.THEME_NAMES;
        int curIdx = 0;
        for (int i = 0; i < names.length; i++) if (names[i].equals(tse.config.theme)) { curIdx = i; break; }
        add(new CycleButton(left, cursorY + 8, 160, 18, Arrays.asList(names), curIdx, i -> {
            tse.config.theme = names[i];
            MCTheme.applyTheme(tse.config.theme);
            tse.saveConfig();
        }));
        add(FlatButton.builder(Component.literal("Edit HUD Positions..."), b ->
                        Minecraft.getInstance().gui.setScreen(new TSEHudEditorScreen(this, null)))
                .dimensions(left + 170, cursorY + 8, 130, 18)
                .style(FlatButton.Style.ACCENT)
                .build());
        cursorY += 40;

        addSection("Utilities");
        addCard(contentX, cursorY, contentW, 34, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, false);
        add(FlatButton.builder(Component.literal("Undo"), b -> { tse.undo(); rebuildContent(); })
                .dimensions(left, cursorY + 8, 64, 18).build());
        add(FlatButton.builder(Component.literal("Save Config"), b -> tse.saveConfig())
                .dimensions(left + 70, cursorY + 8, 90, 18).build());
        add(FlatButton.builder(Component.literal("Refresh Sounds"), b -> { tse.refreshAvailableSounds(); rebuildContent(); })
                .dimensions(left + 166, cursorY + 8, 110, 18).build());
        add(FlatButton.builder(Component.literal("Open Sounds Folder"), b -> {
                    try { new ProcessBuilder("explorer.exe", tse.SOUNDS_DIR.getAbsolutePath()).start(); }
                    catch (Exception ignored) {}
                })
                .dimensions(left + 282, cursorY + 8, 136, 18).build());
        cursorY += 40;

        addSection("Developer");
        addCard(contentX, cursorY, contentW, 34, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, false);
        add(FlatButton.builder(Component.literal("Run Audio Debug"), b -> tse.runAudioDebug())
                .dimensions(left, cursorY + 8, 130, 18).build());
        cursorY += 40;
    }

    private void pageProfiles() {
        int left = contentX;
        cursorY += 4;

        newProfileName.setX(left);
        newProfileName.setY(cursorY);
        newProfileName.setWidth(210);
        add(newProfileName);
        add(FlatButton.builder(Component.literal("Save Current as Profile"), b -> {
                    ProfileManager.INSTANCE.createProfile(newProfileName.getValue(), tse.config);
                    newProfileName.setValue("");
                    rebuildContent();
                })
                .dimensions(left + 218, cursorY, 150, 18)
                .style(FlatButton.Style.ACCENT)
                .build());
        cursorY += 28;

        if (ProfileManager.INSTANCE.profiles.isEmpty()) {
            addLabel(left + 4, cursorY + 6, "No saved profiles yet.");
            cursorY += 24;
            return;
        }

        int cardH = 28;
        for (int i = 0; i < ProfileManager.INSTANCE.profiles.size(); i++) {
            ProfileManager.Profile profile = ProfileManager.INSTANCE.profiles.get(i);
            addCard(left, cursorY, contentW, cardH, MCTheme.CARD_FILL, MCTheme.CARD_BORDER, false);

            add(FlatButton.builder(Component.literal("Load"), b -> {
                        ProfileManager.INSTANCE.applyProfile(profile, tse.config);
                        rebuildContent();
                    })
                    .dimensions(left + contentW - 124, cursorY + 5, 56, 18)
                    .style(FlatButton.Style.ACCENT)
                    .build());
            add(deleteButton(left + contentW - 62, cursorY + 5, 56, 18, () -> {
                ProfileManager.INSTANCE.deleteProfile(profile);
                rebuildContent();
            }));
            profileLabels.add(new String[]{profile.name, String.valueOf(cursorY)});
            cursorY += cardH + 5;
        }
    }

    private final List<String[]> profileLabels = new ArrayList<>();

    private AbstractWidget deleteButton(int x, int y, int w, int h, Runnable action) {
        return FlatButton.builder(Component.literal("Delete"), b -> action.run())
                .dimensions(x, y, w, h)
                .style(FlatButton.Style.DANGER)
                .build();
    }

    private void addSection(String title) {
        cursorY += 4;
        sectionLabels.add(new Object[]{title, cursorY});
        cursorY += 16;
    }

    private void addLabel(int x, int y, String text) {
        inlineLabels.add(new Object[]{text, x, y});
    }

    private int toggleWithLabel(int x, int y, boolean value, String label, java.util.function.Consumer<Boolean> onChange) {
        add(new ToggleWidget(x, y, value, onChange));
        addLabel(x + ToggleWidget.W + 5, y + 2, label);
        int labelW = font.width(label);
        return x + ToggleWidget.W + 5 + labelW + 14;
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
        context.fill(0, 0, width, height, 0x80000000);
        MCTheme.fillRoundedCard(context, winX, winY, winW, winH, 12, MCTheme.WINDOW_BG, MCTheme.CARD_BORDER);

        MCTheme.fillRounded(context, winX + 1, winY + 35, sideW - 1, winH - 36, 10, MCTheme.SIDEBAR_BG);
        context.blit(RenderPipelines.GUI_TEXTURED, LOGO_TEXTURE,
                winX + 10, winY + 5, 0f, 0f, LOGO_W, LOGO_H, LOGO_TEX_W, LOGO_TEX_H, LOGO_TEX_W, LOGO_TEX_H);
        MCTheme.drawTextHD(context, font, PAGES[activePage], contentX, winY + 13, MCTheme.TEXT, false);

        context.fill(winX + 1, winY + 34, winX + winW - 1, winY + 35, MCTheme.BORDER);
        context.fill(winX + sideW, winY + 35, winX + sideW + 1, winY + winH - 1, MCTheme.BORDER);

        context.enableScissor(contentX - SLIDER_KNOB_MARGIN, contentY, contentX + contentW + 4, contentY + contentH);

        for (CardRect c : cardRects) {
            if (c.y + c.h < contentY || c.y > contentY + contentH) continue;
            MCTheme.fillRoundedCard(context, c.x, c.y, c.w, c.h, 5, c.fill, c.border);
            if (c.accentLeft) {
                MCTheme.fillRounded(context, c.x, c.y + 4, 2, c.h - 8, 1, MCTheme.accent(220));
            }
        }

        for (Object[] s : sectionLabels) {
            String title = (String) s[0];
            int y = (int) s[1];
            MCTheme.drawTextHD(context, font, title, contentX + 2, y, MCTheme.accent(255), false);
            context.fill(contentX + 2, y + 11, contentX + contentW, y + 12, MCTheme.BORDER);
        }
        for (Object[] l : inlineLabels) {
            MCTheme.drawTextHD(context, font, (String) l[0], (int) l[1], (int) l[2], MCTheme.TEXT_DIM, false);
        }
        for (String[] p : profileLabels) {
            int y = Integer.parseInt(p[1]);
            MCTheme.drawTextHD(context, font, p[0], contentX + 10, y + 10, MCTheme.TEXT, false);
        }

        for (AbstractWidget w : contentWidgets) {
            w.extractRenderState(context, mouseX, mouseY, delta);
        }
        context.disableScissor();

        if (contentTotalHeight > contentH) {
            int maxScroll = contentTotalHeight - contentH;
            int barX = winX + winW - 5;
            int thumbH = Math.max(20, (int) ((contentH / (float) contentTotalHeight) * contentH));
            int thumbY = contentY + (int) ((scrollOffset / (float) maxScroll) * (contentH - thumbH));
            MCTheme.fillRounded(context, barX, contentY, 2, contentH, 1, MCTheme.BORDER);
            MCTheme.fillRounded(context, barX, thumbY, 2, thumbH, 1, MCTheme.accent(180));
        }

        super.extractRenderState(context, mouseX, mouseY, delta);

        for (AbstractWidget w : contentWidgets) {
            if (w instanceof CycleButton cb && cb.isOpen()) {
                cb.updateHover(mouseX, mouseY);
                cb.renderDropdown(context);
            }
        }

        if (capturingRule != null) {
            context.centeredText(font, "Press a key or mouse button to bind (Esc to cancel)...",
                    winX + winW / 2, winY + winH - 14, 0xFFFFAA33);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {}

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
            return focusedContent.mouseReleased(click);
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
