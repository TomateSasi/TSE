package com.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;

public class ProfileManager {

    public static final ProfileManager INSTANCE = new ProfileManager();

    private static final File PROFILES_FILE = new File(
            Minecraft.getInstance().gameDirectory, "config/tse_profiles.json");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class Profile {
        public String name;

        public Map<String, Boolean> soundRuleStates = new HashMap<>();
        public Map<String, Boolean> chatRuleStates  = new HashMap<>();

        public Profile() {}
        public Profile(String name) { this.name = name; }
    }

    private static class ProfileFile {
        public List<Profile> profiles = new ArrayList<>();
    }

    public final List<Profile> profiles = new ArrayList<>();

    public void load() {
        profiles.clear();
        if (!PROFILES_FILE.exists()) return;
        try (Reader r = new FileReader(PROFILES_FILE)) {
            Type t = new TypeToken<ProfileFile>(){}.getType();
            ProfileFile pf = GSON.fromJson(r, t);
            if (pf != null && pf.profiles != null) {
                for (Profile p : pf.profiles) {
                    if (p.soundRuleStates == null) p.soundRuleStates = new HashMap<>();
                    if (p.chatRuleStates  == null) p.chatRuleStates  = new HashMap<>();
                    profiles.add(p);
                }
            }
        } catch (Exception e) {
            System.err.println("[TSE] Failed to load profiles: " + e.getMessage());
        }
    }

    public void save() {
        try {
            PROFILES_FILE.getParentFile().mkdirs();
        } catch (Exception ignored) {}
        ProfileFile pf = new ProfileFile();
        pf.profiles = new ArrayList<>(profiles);
        try (Writer w = new FileWriter(PROFILES_FILE)) {
            GSON.toJson(pf, w);
        } catch (Exception e) {
            System.err.println("[TSE] Failed to save profiles: " + e.getMessage());
        }
    }

    public Profile createProfile(String name, ModConfig config) {
        Profile p = new Profile(name.trim().isEmpty() ? "Profile " + (profiles.size() + 1) : name.trim());

        for (ModConfig.Category cat : config.categories) {
            for (ModConfig.SoundRule rule : cat.rules) {
                if (rule.ruleId != null && !rule.ruleId.isEmpty()) {
                    p.soundRuleStates.put(rule.ruleId, rule.enabled);
                }
            }
        }
        for (ModConfig.ChatCategory cc : config.chatCategories) {
            for (ModConfig.ChatRule rule : cc.rules) {
                if (rule.ruleId != null && !rule.ruleId.isEmpty()) {
                    p.chatRuleStates.put(rule.ruleId, rule.enabled);
                }
            }
        }

        profiles.add(p);
        save();
        return p;
    }

    public void applyProfile(Profile p, ModConfig config) {
        for (ModConfig.Category cat : config.categories) {
            for (ModConfig.SoundRule rule : cat.rules) {
                if (rule.ruleId == null || rule.ruleId.isEmpty()) continue;

                rule.enabled = p.soundRuleStates.getOrDefault(rule.ruleId, false);
            }
        }
        for (ModConfig.ChatCategory cc : config.chatCategories) {
            for (ModConfig.ChatRule rule : cc.rules) {
                if (rule.ruleId == null || rule.ruleId.isEmpty()) continue;
                rule.enabled = p.chatRuleStates.getOrDefault(rule.ruleId, false);
            }
        }
        tse.saveConfig();
    }

    public void deleteProfile(Profile p) {
        profiles.remove(p);
        save();
    }

    public static void assignIds(ModConfig config) {
        for (int ci = 0; ci < config.categories.size(); ci++) {
            ModConfig.Category cat = config.categories.get(ci);
            for (int ri = 0; ri < cat.rules.size(); ri++) {
                ModConfig.SoundRule rule = cat.rules.get(ri);
                if (rule.ruleId == null || rule.ruleId.isEmpty()) {
                    rule.ruleId = "s_" + ci + "_" + ri + "_"
                            + sanitize(rule.itemKeyword);
                }
            }
        }
        for (int ci = 0; ci < config.chatCategories.size(); ci++) {
            ModConfig.ChatCategory cc = config.chatCategories.get(ci);
            for (int ri = 0; ri < cc.rules.size(); ri++) {
                ModConfig.ChatRule rule = cc.rules.get(ri);
                if (rule.ruleId == null || rule.ruleId.isEmpty()) {
                    rule.ruleId = "c_" + ci + "_" + ri + "_"
                            + sanitize(rule.messageKeyword);
                }
            }
        }
    }

    private static String sanitize(String s) {
        if (s == null || s.isEmpty()) return "rule";
        return s.toLowerCase().replaceAll("[^a-z0-9]", "_").substring(0, Math.min(s.length(), 24));
    }
}
