package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.lwjgl.glfw.GLFW;

import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class tse implements ClientModInitializer {
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean awaitingLocrawResponse = new AtomicBoolean(false);

    private static final File CONFIG_FILE = new File(
            Minecraft.getInstance().gameDirectory, "config/tse_config.json");
    public static final File SOUNDS_DIR = new File(
            Minecraft.getInstance().gameDirectory, "config/tse_sounds");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static ModConfig    config          = new ModConfig();
    public static String       currentLocation = "none";
    public static List<String> availableSounds = new ArrayList<>();

    private static final Deque<ModConfig> undoStack = new ArrayDeque<>();
    private static final int MAX_UNDO = 20;

    private boolean wasUsingBefore  = false;
    private boolean wasAttackBefore = false;
    private boolean wasMiddleBefore = false;
    private final Set<Integer> pressedKeys     = new HashSet<>();
    private final Set<Integer> prevPressedKeys = new HashSet<>();

    public static boolean voidgloomSneakActive = false;

    private static final AtomicBoolean voidgloomLoopRunning = new AtomicBoolean(false);
    private static Thread voidgloomReminderThread = null;

    private static final Map<ModConfig.ChatRule, Thread>        chatLoopThreads = new ConcurrentHashMap<>();
    private static final Map<ModConfig.ChatRule, AtomicBoolean> chatLoopFlags   = new ConcurrentHashMap<>();

    private static final Random RNG = new Random();

    public static final SoundEvent CUSTOM_SOUND = Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            Identifier.fromNamespaceAndPath("tse", "custom_sound"),
            SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("tse", "custom_sound")));

    public static void pushUndo() {
        undoStack.push(config.copy());
        if (undoStack.size() > MAX_UNDO) undoStack.pollLast();
    }
    public static boolean undo() {
        if (undoStack.isEmpty()) return false;
        config = undoStack.pop(); return true;
    }
    public static boolean canUndo() { return !undoStack.isEmpty(); }

    @Override
    public void onInitializeClient() {
        if (!SOUNDS_DIR.exists()) SOUNDS_DIR.mkdirs();
        loadConfig();
        refreshAvailableSounds();

        net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback.EVENT.register((dispatcher, reg) ->
                dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>literal("tse")
                        .executes(ctx -> {
                            Minecraft.getInstance().execute(() -> net.tse.TSEGui.INSTANCE.toggleVisible());
                            return 1;
                        })
                        .then(com.mojang.brigadier.builder.LiteralArgumentBuilder.<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>literal("enable")
                                .executes(ctx -> {
                                    config.masterEnabled = true;
                                    saveConfig();
                                    return 1;
                                }))
                        .then(com.mojang.brigadier.builder.LiteralArgumentBuilder.<net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource>literal("disable")
                                .executes(ctx -> {
                                    config.masterEnabled = false;
                                    saveConfig();
                                    return 1;
                                }))
                )
        );

        net.tse.ui.MCTheme.applyTheme(config.theme);

        ClientReceiveMessageEvents.CHAT.register(
                (msg, signed, sender, params, ts) -> handleChatMessage(msg.getString(), false));
        ClientReceiveMessageEvents.GAME.register(
                (msg, overlay) -> { if (!overlay) handleChatMessage(msg.getString(), true); });
        ClientReceiveMessageEvents.ALLOW_GAME.register((msg, overlay) -> {
            String text = msg.getString();
            if (text.contains("Sending to server")) {
                awaitingLocrawResponse.set(true);
                scheduler.schedule(() -> Minecraft.getInstance().execute(() -> {
                    if (Minecraft.getInstance().player != null)
                        Minecraft.getInstance().player.connection.sendCommand("locraw");
                }), 3, TimeUnit.SECONDS);
            }
            if (awaitingLocrawResponse.get() && text.startsWith("{") && text.contains("\"mode\":\"")) {
                try { currentLocation = text.split("\"mode\":\"")[1].split("\"")[0]; }
                catch (Exception ignored) {}
                awaitingLocrawResponse.set(false);
                return false;
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);

        HudElementRegistry.attachElementAfter(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("tse", "tse_hud"),
                this::renderHud
        );
        registerLogAppender();
    }

    private void onTick(Minecraft client) {
        if (!config.masterEnabled) return;
        if (client.player == null) return;

        boolean isUsing    = client.options.keyUse.isDown();
        boolean isAttack   = client.options.keyAttack.isDown();
        boolean isMiddle   = client.options.keyPickItem.isDown();
        boolean isSneaking = client.player.isShiftKeyDown();

        prevPressedKeys.clear();
        prevPressedKeys.addAll(pressedKeys);
        pressedKeys.clear();

        long window = client.getWindow().handle();
        for (ModConfig.Category cat : config.categories)
            for (ModConfig.SoundRule rule : cat.rules)
                if (rule.keybind.startsWith("KEY_"))
                    try {
                        int c = Integer.parseInt(rule.keybind.substring(4));
                        if (GLFW.glfwGetKey(window, c) == GLFW.GLFW_PRESS) pressedKeys.add(c);
                    } catch (Exception ignored) {}

        ItemStack stack    = client.player.getMainHandItem();
        String    itemName = stack.getHoverName().getString().toLowerCase();

        for (ModConfig.Category cat : config.categories) {
            for (ModConfig.SoundRule rule : cat.rules) {
                if (!rule.enabled || rule.isOnCooldown) continue;
                if (!itemName.contains(rule.itemKeyword.toLowerCase())) continue;
                if (!currentLocation.contains(rule.locationKeyword.toLowerCase())) continue;
                if (rule.sneakOnly && !isSneaking) continue;

                boolean triggered = switch (rule.keybind) {
                    case "RIGHT_CLICK"  -> isUsing  && !wasUsingBefore;
                    case "LEFT_CLICK"   -> isAttack && !wasAttackBefore;
                    case "MIDDLE_CLICK" -> isMiddle && !wasMiddleBefore;
                    default -> {
                        if (rule.keybind.startsWith("KEY_"))
                            try {
                                int c = Integer.parseInt(rule.keybind.substring(4));
                                yield pressedKeys.contains(c) && !prevPressedKeys.contains(c);
                            } catch (Exception ignored) {}
                        yield false;
                    }
                };
                if (triggered) {
                    rule.lastItemName = stack.getHoverName().getString();
                    triggerRule(rule);
                }
            }
        }

        boolean shouldLoop = voidgloomSneakActive && config.voidgloom.enabled && !isSneaking;
        if (shouldLoop && !voidgloomLoopRunning.get()) {
            startVoidgloomLoop();
        } else if (!shouldLoop && voidgloomLoopRunning.get()) {
            stopVoidgloomLoop();
        }

        wasUsingBefore  = isUsing;
        wasAttackBefore = isAttack;
        wasMiddleBefore = isMiddle;
    }

    private static void startVoidgloomLoop() {
        if (!voidgloomLoopRunning.compareAndSet(false, true)) return;
        voidgloomReminderThread = new Thread(() -> {
            while (voidgloomLoopRunning.get()) {

                String soundFile = config.voidgloom.reminderSound;
                int    volume    = config.voidgloom.reminderVolume;
                playSoundBlocking(soundFile, volume);

            }
        }, "TSE-Voidgloom-Loop");
        voidgloomReminderThread.setDaemon(true);
        voidgloomReminderThread.start();
    }

    private static void stopVoidgloomLoop() {
        voidgloomLoopRunning.set(false);

        voidgloomReminderThread = null;
    }

    private static void playSoundBlocking(String soundFileName, int volume) {
        float gainFactor = volume / 100.0f;

        if (EmbeddedSounds.SOUNDS.containsKey(soundFileName)) {
            String path = EmbeddedSounds.SOUNDS.get(soundFileName);
            if (path != null) try { decodeAndPlayResource(path, gainFactor); } catch (Exception ignored) {}
            return;
        }

        if (soundFileName == null || soundFileName.equals("Meow") || soundFileName.isEmpty()) {
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player != null)
                    Minecraft.getInstance().player.playSound(
                            CUSTOM_SOUND, Math.min(1.0f, gainFactor), 1.0f);
            });
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            return;
        }

        File file = new File(SOUNDS_DIR, soundFileName);
        if (!file.exists()) {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            return;
        }
        decodeAndPlay(file, gainFactor, true);
    }

    private static void decodeAndPlay(File source, float gain, boolean blocking) {
        if (source == null || !source.exists()) {
            System.err.println("[TSE] File not found: " + (source == null ? "null" : source.getAbsolutePath()));
            return;
        }
        try {
            AudioInputStream audioIn;
            try {
                audioIn = AudioSystem.getAudioInputStream(source);
            } catch (Exception e1) {
                audioIn = AudioSystem.getAudioInputStream(
                        new BufferedInputStream(new FileInputStream(source), 65536));
            }
            playStream(audioIn, gain);
        } catch (Exception e) {
            System.err.println("[TSE] Error playing " + source.getName() + ": " + e);
            e.printStackTrace();
        }
    }

    private static void decodeAndPlayResource(String resourcePath, float gain) {
        InputStream res = EmbeddedSounds.class.getResourceAsStream(resourcePath);
        if (res == null) { System.err.println("[TSE] Resource not found: " + resourcePath); return; }
        try {
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(
                    new BufferedInputStream(res, 65536));
            playStream(audioIn, gain);
        } catch (Exception e) {
            System.err.println("[TSE] Error playing resource " + resourcePath + ": " + e);
            e.printStackTrace();
        }
    }

    private static void playStream(AudioInputStream audioIn, float gain) throws Exception {
        AudioFormat srcFmt = audioIn.getFormat();
        AudioInputStream pcmIn;

        if (srcFmt.getEncoding() == AudioFormat.Encoding.PCM_SIGNED
                || srcFmt.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
            pcmIn = audioIn;
        } else {

            int ch = srcFmt.getChannels() > 0 ? srcFmt.getChannels() : 2;
            float sr = srcFmt.getSampleRate() > 0 ? srcFmt.getSampleRate() : 44100f;
            AudioFormat pcmFmt = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    sr, AudioSystem.NOT_SPECIFIED,
                    ch, AudioSystem.NOT_SPECIFIED,
                    sr, false);
            pcmIn = AudioSystem.getAudioInputStream(pcmFmt, audioIn);
        }

        AudioFormat decoded = pcmIn.getFormat();
        int ch  = decoded.getChannels()   > 0 ? decoded.getChannels()   : 2;
        float sr = decoded.getSampleRate() > 0 ? decoded.getSampleRate() : 44100f;

        AudioFormat playFmt = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sr, 16, ch, ch * 2, sr, false);

        AudioInputStream playIn = new AudioInputStream(pcmIn, playFmt, pcmIn.getFrameLength());

        DataLine.Info info = new DataLine.Info(SourceDataLine.class, playFmt);
        SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(playFmt);
        applyGain(line, gain);
        line.start();
        byte[] buf = new byte[4096]; int read;
        while ((read = playIn.read(buf)) != -1) line.write(buf, 0, read);
        line.drain(); line.close();
        playIn.close();
    }

    private static void applyGain(SourceDataLine line, float gainFactor) {
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = gainFactor > 0
                    ? (float)(20.0 * Math.log10(gainFactor))
                    : gain.getMinimum();
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB)));
        }
    }

    private void handleChatMessage(String rawText, boolean isSystem) {
        if (!config.masterEnabled) return;
        String text = rawText.toLowerCase();

        for (ModConfig.ChatCategory cat : config.chatCategories) {
            for (ModConfig.ChatRule rule : cat.rules) {
                if (!rule.enabled) continue;
                if (rule.systemOnly && !isSystem) continue;
                String kw = rule.messageKeyword.toLowerCase();
                if (kw.isEmpty()) continue;

                boolean matches = rule.exactMatch ? text.equals(kw) : text.contains(kw);

                if (chatLoopFlags.containsKey(rule)) {
                    String end = rule.loopEndTrigger.toLowerCase().trim();
                    if (!end.isEmpty()) {
                        boolean endMatches = rule.exactMatch ? text.equals(end) : text.contains(end);
                        if (endMatches) { stopChatLoop(rule); continue; }
                    }
                }

                if (!matches) continue;

                if (rule.loopEnabled) {

                    if (!chatLoopFlags.containsKey(rule)) {
                        startChatLoop(rule);
                    }
                } else {
                    playSoundWithSecret(rule.soundFile, rule.volume);
                }
            }
        }
    }

    private static void startChatLoop(ModConfig.ChatRule rule) {
        AtomicBoolean flag = new AtomicBoolean(true);
        chatLoopFlags.put(rule, flag);
        Thread t = new Thread(() -> {
            while (flag.get()) {
                playSoundBlocking(rule.soundFile, rule.volume);
            }
        }, "TSE-Chat-Loop");
        t.setDaemon(true);
        chatLoopThreads.put(rule, t);
        t.start();
    }

    public static void stopChatLoop(ModConfig.ChatRule rule) {
        AtomicBoolean flag = chatLoopFlags.remove(rule);
        if (flag != null) flag.set(false);
        Thread t = chatLoopThreads.remove(rule);
        if (t != null) t.interrupt();
    }

    private void triggerRule(ModConfig.SoundRule rule) {
        rule.isOnCooldown = true;
        rule.timerStartMs = System.currentTimeMillis();
        rule.timerActive  = true;

        scheduler.schedule(() -> {
            rule.isOnCooldown = false;
            rule.timerActive  = false;
            if (rule.overlayEnabled)
                rule.overlayShowUntilMs = System.currentTimeMillis() + rule.overlayDurationMs;
            playSoundWithSecret(rule.soundFile, rule.volume);
        }, rule.delaySeconds, TimeUnit.SECONDS);
    }

    static void playSoundWithSecret(String soundFile, int volume) {
        if (RNG.nextInt(1_000_000) == 0 && EmbeddedSounds.secretExists()) {
            EmbeddedSounds.playSecret();
            return;
        }

        java.time.MonthDay _d = java.time.MonthDay.now();
        if (_d.getMonthValue() == 4 && _d.getDayOfMonth() == 1 && EmbeddedSounds.aprilFoolsExists()) {
            EmbeddedSounds.playAprilFools(volume);
            return;
        }
        playSound(soundFile, volume);
    }

    public static void playSound(String soundFileName, int volume) {
        float gainFactor = volume / 100.0f;

        if (EmbeddedSounds.SOUNDS.containsKey(soundFileName)) {
            EmbeddedSounds.play(soundFileName, volume);
            return;
        }

        if (soundFileName == null || soundFileName.equals("Meow") || soundFileName.isEmpty()) {
            Minecraft.getInstance().execute(() -> {
                if (Minecraft.getInstance().player != null)
                    Minecraft.getInstance().player.playSound(
                            CUSTOM_SOUND, Math.min(1.0f, gainFactor), 1.0f);
            });
            return;
        }

        File file = new File(SOUNDS_DIR, soundFileName);
        if (!file.exists()) { playSound("Meow", volume); return; }

        new Thread(() -> decodeAndPlay(file, gainFactor, false), "TSE-Sound-Player").start();
    }

    public static void playSound(String f) { playSound(f, 100); }

    public static void refreshAvailableSounds() {
        availableSounds.clear();
        availableSounds.add("Meow");

        for (String name : EmbeddedSounds.SOUNDS.keySet())
            availableSounds.add(name);
        if (SOUNDS_DIR.exists()) {
            File[] files = SOUNDS_DIR.listFiles((d, n) -> {
                String low = n.toLowerCase();
                if (low.equals("ngg.ogg")) return false;
                return low.endsWith(".ogg") || low.endsWith(".wav") || low.endsWith(".mp3");
            });
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) availableSounds.add(f.getName());
            }
        }
    }

    private void renderHud(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.gui.hud.isHidden()) return;

        int  screenW   = client.getWindow().getGuiScaledWidth();
        long now       = System.currentTimeMillis();
        boolean isSneaking = client.player.isShiftKeyDown();

        if (voidgloomSneakActive && config.voidgloom.enabled
                && config.voidgloom.overlayEnabled && !isSneaking) {
            boolean show = !config.voidgloom.overlayBlink || (now / 400) % 2 == 0;
            if (show) {
                int color = config.voidgloom.overlayRainbow
                        ? java.awt.Color.HSBtoRGB((now % 3000L) / 3000.0f, 0.8f, 1.0f)
                        : config.voidgloom.overlayColor;
                drawHudText(context, client, config.voidgloom.overlayText,
                        config.voidgloom.overlayX, config.voidgloom.overlayY,
                        config.voidgloom.overlayScale, color, screenW);
            }
        }

        for (ModConfig.Category cat : config.categories) {
            for (ModConfig.SoundRule rule : cat.rules) {
                if (!rule.enabled) continue;

                if (rule.overlayEnabled && now < rule.overlayShowUntilMs) {
                    int color = rule.overlayRainbow
                            ? java.awt.Color.HSBtoRGB((now % 3000L) / 3000.0f, 0.8f, 1.0f)
                            : rule.overlayColor;
                    drawHudText(context, client, rule.overlayText,
                            rule.overlayX, rule.overlayY, rule.overlayScale, color, screenW);
                }

                if (rule.timerActive) {
                    long elapsed   = (now - rule.timerStartMs) / 1000L;
                    long remaining = Math.max(0, rule.delaySeconds - elapsed);

                    if (rule.closeWarnEnabled
                            && remaining <= rule.closeWarnSecondsBeforeEnd
                            && remaining > 0) {
                        boolean show = !rule.closeWarnBlink || (now / 400) % 2 == 0;
                        if (show) {
                            int color = rule.closeWarnRainbow
                                    ? java.awt.Color.HSBtoRGB((now % 3000L) / 3000.0f, 0.8f, 1.0f)
                                    : rule.closeWarnColor;
                            drawHudText(context, client, rule.closeWarnText,
                                    rule.closeWarnX, rule.closeWarnY,
                                    rule.closeWarnScale, color, screenW);
                        }
                    }

                    int warnBorder = rule.closeWarnEnabled ? rule.closeWarnSecondsBeforeEnd : 3;
                    int timerColor = remaining > warnBorder ? 0xFF88FF88
                            : remaining > 1         ? 0xFFFFDD44 : 0xFFFF4444;
                    String line = remaining + "s"
                            + (rule.lastItemName.isEmpty() ? "" : " | " + rule.lastItemName);
                    context.pose().pushMatrix();
                    context.pose().scale(0.75f, 0.75f);
                    context.text(client.font, line,
                            (int)(10 / 0.75f), (int)(20 / 0.75f), timerColor);
                    context.pose().popMatrix();
                }
            }
        }
    }

    public static void drawHudText(GuiGraphicsExtractor ctx, Minecraft client,
                                   String rawText, int x, int y, float scale, int color, int screenW) {
        String converted = rawText.replace('&', '\u00a7');
        net.minecraft.network.chat.MutableComponent text = net.minecraft.network.chat.Component.literal(converted);
        ctx.pose().pushMatrix();
        ctx.pose().scale(scale, scale);
        if (x < 0)
            ctx.centeredText(client.font, text,
                    (int)(screenW / scale / 2), (int)(y / scale), color);
        else
            ctx.text(client.font, text,
                    (int)(x / scale), (int)(y / scale), color);
        ctx.pose().popMatrix();
    }

    private static void chat(String msg) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player != null)
                mc.player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
        });
    }

    public static void runAudioDebug() {
        new Thread(() -> {
            chat("§e[TSE Debug] §fStarting audio diagnostic...");

            chat("§7Java: " + System.getProperty("java.version")
                    + "  OS: " + System.getProperty("os.name"));

            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            chat("§7Mixers found: §f" + mixers.length);
            for (Mixer.Info m : mixers)
                chat("  §8> §7" + m.getName() + " — " + m.getDescription());

            AudioFormat testFmt = new AudioFormat(44100, 16, 2, true, false);
            DataLine.Info testInfo = new DataLine.Info(SourceDataLine.class, testFmt);
            boolean lineSupported = AudioSystem.isLineSupported(testInfo);
            chat("§7SourceDataLine (44100/16/stereo) supported: "
                    + (lineSupported ? "§aYES" : "§cNO — this is why no sound plays"));

            if (lineSupported) {
                try {
                    SourceDataLine testLine = (SourceDataLine) AudioSystem.getLine(testInfo);
                    testLine.open(testFmt);
                    testLine.close();
                    chat("§7Line open/close test: §aOK");
                } catch (Exception e) {
                    chat("§7Line open/close test: §cFAILED — " + e.getMessage());
                }
            }

            chat("§7SOUNDS_DIR: §f" + SOUNDS_DIR.getAbsolutePath());
            chat("§7SOUNDS_DIR exists: " + (SOUNDS_DIR.exists() ? "§aYES" : "§cNO"));
            if (SOUNDS_DIR.exists()) {
                File[] files = SOUNDS_DIR.listFiles();
                chat("§7Files in sounds dir: §f" + (files == null ? 0 : files.length));
                if (files != null) for (File f : files) {
                    chat("  §8> §7" + f.getName() + " (" + f.length() + " bytes)");
                    try {
                        AudioInputStream ais = AudioSystem.getAudioInputStream(f);
                        chat("    §aReadable — format: " + ais.getFormat());
                        ais.close();
                    } catch (Exception e) {

                        try {
                            AudioInputStream ais2 = AudioSystem.getAudioInputStream(
                                    new BufferedInputStream(new FileInputStream(f), 65536));
                            chat("    §aReadable via BufferedInputStream — format: " + ais2.getFormat());
                            ais2.close();
                        } catch (Exception e2) {
                            chat("    §cNOT readable: " + e.getMessage());
                        }
                    }
                }
            }

            chat("§7Embedded sounds: §f" + EmbeddedSounds.SOUNDS.size());
            for (Map.Entry<String, String> e : EmbeddedSounds.SOUNDS.entrySet()) {
                InputStream s = EmbeddedSounds.class.getResourceAsStream(e.getValue());
                chat("  §8> §7" + e.getKey() + " → "
                        + (s != null ? "§afound in jar" : "§cMISSING from jar"));
                if (s != null) try { s.close(); } catch (Exception ignored) {}
            }

            chat("§7availableSounds list (" + availableSounds.size() + " entries):");
            for (String s : availableSounds) chat("  §8> §7" + s);

            chat("§7Running blocking audio test...");
            new Thread(() -> {

                try {
                    InputStream res = EmbeddedSounds.class.getResourceAsStream("/assets/tse/sounds/Anvil.wav");
                    if (res == null) { chat("§cAnvil.wav not in jar"); }
                    else {
                        AudioInputStream ai = AudioSystem.getAudioInputStream(new BufferedInputStream(res, 65536));
                        chat("§7Anvil format: §f" + ai.getFormat());
                        playStream(ai, 1.0f);
                        chat("§aAnvil.wav played OK");
                    }
                } catch (Exception e) { chat("§cAnvil.wav FAILED: " + e.getMessage()); }

                File[] oggFiles = SOUNDS_DIR.listFiles((d,n) -> n.toLowerCase().endsWith(".ogg"));
                if (oggFiles != null && oggFiles.length > 0) {
                    try {
                        AudioInputStream ai = AudioSystem.getAudioInputStream(oggFiles[0]);
                        chat("§7OGG format after getAudioInputStream: §f" + ai.getFormat());
                        playStream(ai, 1.0f);
                        chat("§a" + oggFiles[0].getName() + " played OK");
                    } catch (Exception e) { chat("§c" + oggFiles[0].getName() + " FAILED: " + e.getMessage()); }
                }

                File[] mp3Files = SOUNDS_DIR.listFiles((d,n) -> n.toLowerCase().endsWith(".mp3"));
                if (mp3Files != null && mp3Files.length > 0) {
                    try {
                        AudioInputStream ai = AudioSystem.getAudioInputStream(mp3Files[0]);
                        chat("§7MP3 format after getAudioInputStream: §f" + ai.getFormat());
                        playStream(ai, 1.0f);
                        chat("§a" + mp3Files[0].getName() + " played OK");
                    } catch (Exception e) { chat("§c" + mp3Files[0].getName() + " FAILED: " + e.getMessage()); }
                }

                chat("§e[TSE Debug] §fAudio test complete.");
            }, "TSE-Debug-Play").start();
        }, "TSE-Debug").start();
    }

    public static void saveConfig() {
        ProfileManager.assignIds(config);
        try (Writer w = new FileWriter(CONFIG_FILE)) { GSON.toJson(config, w); }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void loadConfig() {
        if (CONFIG_FILE.exists()) {
            try (Reader r = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(r, ModConfig.class);
                if (config == null)                 config    = new ModConfig();
                if (config.categories == null)      config.categories = new ArrayList<>();
                if (config.chatRules  == null)      config.chatRules  = new ArrayList<>();
                if (config.chatCategories == null)  config.chatCategories = new ArrayList<>();
                if (config.voidgloom  == null)      config.voidgloom  = new ModConfig.VoidgloomSettings();
                if (config.voidgloom.reminderSound  == null) config.voidgloom.reminderSound  = "Meow";
                if (config.voidgloom.overlayText    == null) config.voidgloom.overlayText    = "&cSNEAK!";
                for (ModConfig.Category cat : config.categories) {
                    if (cat.rules == null) cat.rules = new ArrayList<>();
                    for (ModConfig.SoundRule rule : cat.rules) {
                        if (rule.keybind   == null || rule.keybind.isEmpty())   rule.keybind   = "RIGHT_CLICK";
                        if (rule.soundFile == null || rule.soundFile.isEmpty()) rule.soundFile = "Meow";
                        if (rule.overlayText   == null) rule.overlayText   = "NOW!";
                        if (rule.closeWarnText == null) rule.closeWarnText = "GET READY!";
                    }
                }
                for (ModConfig.ChatCategory cc : config.chatCategories) {
                    if (cc.rules == null) cc.rules = new ArrayList<>();
                    for (ModConfig.ChatRule cr : cc.rules) {
                        if (cr.soundFile == null || cr.soundFile.isEmpty()) cr.soundFile = "Meow";
                        if (cr.loopEndTrigger == null) cr.loopEndTrigger = "";
                    }
                }

                config.migrateLegacyChatRules();
                if (config.categories.isEmpty()) createDefaultCategory();
                ProfileManager.assignIds(config);
                ProfileManager.INSTANCE.load();
            } catch (Exception e) { e.printStackTrace(); }
        } else { createDefaultCategory(); saveConfig(); }
    }

    private void createDefaultCategory() {
        ModConfig.Category def = new ModConfig.Category("Default");
        def.rules.add(new ModConfig.SoundRule("item1", "dungeon", 5, true));
        config.categories.add(def);
    }

    private void registerLogAppender() {
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        AbstractAppender appender = new AbstractAppender(
                "TSE_Scanner", null, null, false, null) {
            @Override
            public void append(org.apache.logging.log4j.core.LogEvent event) {
                String msg = event.getMessage().getFormattedMessage();
                if (msg.contains("\"mode\":\""))
                    try { currentLocation = msg.split("\"mode\":\"")[1].split("\"")[0]; }
                    catch (Exception ignored) {}

                if (config.voidgloom.enabled) {
                    String lower = msg.toLowerCase();
                    if (lower.contains("voidgloom") && lower.contains("slay the boss"))
                        voidgloomSneakActive = true;
                    if (lower.contains("slayer quest completed") || lower.contains("slayer quest started"))
                        voidgloomSneakActive = false;
                }
            }
        };
        appender.start();
        ctx.getConfiguration().addAppender(appender);
        ctx.getConfiguration().getRootLogger().addAppender(appender, null, null);
    }
}
