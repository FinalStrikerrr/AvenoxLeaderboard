package com.yourname.universalleaderboards.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GuiConfigManager {

    public static final class ButtonConfig {
        public int slot;
        public String material;
        public String name;
        public List<String> lore;
        public boolean glow;
        public Integer customModelData;
    }

    /** A /stats field: like ButtonConfig, but with an optional %value% format string.
     *  valueFormat == null means this entry is a plain button (e.g. "close"), not a stat display. */
    public static final class StatsItemConfig {
        public int slot;
        public String material;
        public String name;
        public String valueFormat;
        public List<String> lore;
    }

    private final Plugin plugin;
    private FileConfiguration gui;
    private File guiFile;

    // main menu
    private final Map<String, ButtonConfig> mainButtons = new LinkedHashMap<>();
    // board view (shared layout for every leaderboard's top-N screen)
    private final Map<String, ButtonConfig> boardViewButtons = new LinkedHashMap<>();
    // /stats - a single flat "items" map (stat fields and the close button together)
    private final Map<String, StatsItemConfig> statsItems = new LinkedHashMap<>();
    private final Map<String, String> colors = new LinkedHashMap<>();
    private final Map<Integer, String> topIcons = new LinkedHashMap<>();

    public GuiConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            plugin.saveResource("gui.yml", false);
        } else {
            FileConfiguration existing = YamlConfiguration.loadConfiguration(guiFile);
            if (existing.getInt("gui-version", 0) < 7) {
                try {
                    plugin.saveResource("gui.yml", true);
                    plugin.getLogger().info("AvenoxLeaderboard: updated gui.yml to GUI version 7.");
                } catch (Exception ex) {
                    plugin.getLogger().warning("AvenoxLeaderboard: could not update gui.yml: " + ex.getMessage());
                }
            }
        }
        gui = YamlConfiguration.loadConfiguration(guiFile);

        mainButtons.clear();
        readButtons(gui.getConfigurationSection("main.buttons"), mainButtons);

        boardViewButtons.clear();
        readButtons(gui.getConfigurationSection("board-view.buttons"), boardViewButtons);

        statsItems.clear();
        ConfigurationSection itemsSection = gui.getConfigurationSection("stats.items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
                if (itemSection == null) continue;
                StatsItemConfig item = new StatsItemConfig();
                item.slot = itemSection.getInt("slot");
                item.material = itemSection.getString("material", "STONE");
                item.name = itemSection.getString("name", "");
                item.valueFormat = itemSection.getString("value-format", null);
                item.lore = itemSection.getStringList("lore");
                statsItems.put(key, item);
            }
        }

        colors.clear();
        ConfigurationSection colorsSection = gui.getConfigurationSection("colors");
        if (colorsSection != null) {
            for (String key : colorsSection.getKeys(false)) {
                colors.put(key, colorsSection.getString(key, ""));
            }
        }

        topIcons.clear();
        ConfigurationSection topIconsSection = gui.getConfigurationSection("top-icons");
        if (topIconsSection != null) {
            for (String key : topIconsSection.getKeys(false)) {
                try {
                    topIcons.put(Integer.parseInt(key), topIconsSection.getString(key));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    private void readButtons(ConfigurationSection section, Map<String, ButtonConfig> target) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            ConfigurationSection buttonSection = section.getConfigurationSection(key);
            if (buttonSection == null) continue;
            ButtonConfig button = new ButtonConfig();
            button.slot = buttonSection.getInt("slot");
            button.material = buttonSection.getString("material", "STONE");
            button.name = buttonSection.getString("name", "");
            button.lore = buttonSection.getStringList("lore");
            button.glow = buttonSection.getBoolean("glow", false);
            if (buttonSection.contains("custom-model-data")) button.customModelData = buttonSection.getInt("custom-model-data");
            target.put(key, button);
        }
    }

    public void reload() { load(); }

    // --- Main menu ---
    public String getMainTitle() { return gui.getString("main.title", "&b&lLEADERBOARDS"); }
    public int getMainSize() { return gui.getInt("main.size", 45); }
    public Map<String, ButtonConfig> getMainButtons() { return mainButtons; }

    // --- Board view (the top-N screen shared by every leaderboard) ---
    public int getEntriesPerPage() { return gui.getInt("board-view.entries-per-page", 10); }
    public List<Integer> getEntrySlots() { return gui.getIntegerList("board-view.entry-slots"); }
    public boolean usePlayerHeads() { return gui.getBoolean("board-view.use-player-heads", true); }
    public List<String> getEntryLore() { return gui.getStringList("board-view.entry-lore"); }
    public ButtonConfig getBoardViewButton(String key) { return boardViewButtons.get(key); }
    public String getTopIcon(int rank) { return topIcons.get(rank); }

    // --- /stats ---
    public String getStatsTitle() { return gui.getString("stats.title", "&b&lYOUR STATS"); }
    public int getStatsSize() { return gui.getInt("stats.size", 45); }
    public Map<String, StatsItemConfig> getStatsItems() { return statsItems; }

    // --- Shared ---
    public String getColor(String key) { return colors.getOrDefault(key, ""); }
    public boolean isDecorationEnabled() { return gui.getBoolean("decoration.enabled", false); }
    public String getDecorationMaterial() { return gui.getString("decoration.material", "GRAY_STAINED_GLASS_PANE"); }
    public String getSound(String key) { return gui.getString("sounds." + key, null); }
}
