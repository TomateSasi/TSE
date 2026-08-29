# TSE Mod

A client-side Fabric mod for Minecraft that plays custom sounds and shows on-screen warnings in response to what you're holding, where you are, what you type/see in chat, and (for Hypixel SkyBlock players) whether you're sneaking during a Voidgloom Seraph fight. Everything is configured in-game through a built-in settings GUI no config file editing required.

## Requirements

- **Minecraft:** 26.1.2
- **Fabric Loader:** 0.19.3+
- **Fabric API:** required
- Client-side only no server installation needed.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.1.2.
2. Download [Fabric API](https://modrinth.com/mod/fabric-api) and place it in your `mods` folder.
3. Drop the TSE mod jar into your `mods` folder.
4. Launch the game and run **`/tse`** to open the settings menu.

## Features

### 🔊 Sound Rules
Trigger a sound when you use an item in a specific place:
- Match by **item name keyword** (checks your held item's display name) and **location keyword** (matched against your current SkyBlock area/mode).
- Choose the trigger: **Left Click, Right Click, Middle Click, or any keybind** (captured live by pressing the key).
- Optional **sneak-only** condition.
- Configurable **delay/cooldown** in seconds before the sound plays after triggering.
- Organize rules into **collapsible, pinnable categories**.
- Per-rule **on-screen trigger overlay**: custom text, color, scale, position, duration, and an optional rainbow color cycle.
- Per-rule **"close warning" overlay**: a second warning shown some number of seconds before the timer ends, with its own text/color/scale/position and optional blink or rainbow effect.
- A live countdown timer HUD shows remaining cooldown time plus the item name that triggered it.

### 💬 Chat Triggers
Play a sound when a chat or game message matches a keyword:
- **Contains** or **exact match** matching.
- Optional **system message only** filter (e.g. action bar/toast messages vs. player chat).
- **Looping mode**: keeps replaying the sound back-to-back (gapless) until a configurable **end trigger** phrase appears in chat.
- Organized into collapsible, pinnable categories, same as Sound Rules.

### 👁 Voidgloom Seraph Sneak Reminder
A dedicated helper for the Hypixel SkyBlock Voidgloom Seraph slayer boss:
- Automatically detects the fight starting/ending from chat/log messages.
- Plays a **gapless looping reminder sound** whenever you should be sneaking but aren't, stopping instantly once you sneak.
- Configurable reminder sound and volume.
- On-screen warning text with custom color, scale, position, optional blink, and optional rainbow color cycle.

### 🎛 In-Game Settings GUI (`/tse`)
A full settings screen built directly on Minecraft's own renderer (no external UI library):
- **Sound Rules**, **Chat Triggers**, **Voidgloom Seraph**, **General**, and **Profiles** pages, each with search boxes for quickly finding rules.
- Custom flat-style buttons, toggles, sliders, cycle buttons, color picker, and text fields matching a cohesive in-house theme.
- Master **enable/disable** toggle (`/tse enable` / `/tse disable`) to instantly turn off all processing.
- Selectable **UI theme/accent color**, saved between sessions.
- **Undo** support (up to 20 steps) for configuration changes made in the GUI.

### 📁 Profiles
Save and load named snapshots of which rules are enabled:
- Create a profile to capture the current on/off state of every Sound Rule and Chat Trigger.
- Switching to a profile restores those enabled/disabled states; any rule not present in the profile falls back to **disabled**, so new rules never turn on unexpectedly.
- Profiles are stored in `config/tse_profiles.json`.

### 🎵 Sounds
- Two sounds are **built into the mod jar** out of the box (`Meow`, `yippee`, `Anvil`) so it works with zero setup.
- Drop your own `.wav`, `.ogg`, or `.mp3` files into `config/tse_sounds/` and they'll automatically appear in every sound picker in the GUI.
- Supports WAV, OGG/Vorbis, and MP3 decoding (via bundled `tritonus`, `vorbisspi`, `jorbis`, and `mp3spi` libraries).
- Per-rule **volume control**.

## Configuration Files

All data is stored in your Minecraft instance's `config` folder:

| File | Purpose |
|---|---|
| `config/tse_config.json` | All Sound Rules, Chat Triggers, Voidgloom settings, theme, and master toggle |
| `config/tse_profiles.json` | Saved rule-enabled profiles |
| `config/tse_sounds/` | Your custom `.wav` / `.ogg` / `.mp3` sound files |

You normally shouldn't need to hand-edit these everything is manageable from the `/tse` GUI but they're plain, pretty-printed JSON if you ever need to.

## Commands

| Command | Effect |
|---|---|
| `/tse` | Opens/closes the settings GUI |
| `/tse enable` | Turns the mod's processing on |
| `/tse disable` | Turns the mod's processing off |


## Building from Source

```
./gradlew build
```
The compiled jar will be output to `build/libs/`.
