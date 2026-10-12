package com.yourname.universalleaderboards.config;

import com.yourname.universalleaderboards.leaderboard.LeaderboardType;
import com.yourname.universalleaderboards.util.NumberFormatter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final Plugin plugin;
    private FileConfiguration config;
    private FileConfiguration storageConfig;
    private File configFile;
    private File storageFile;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "config.yml");
        storageFile = new File(plugin.getDataFolder(), "storage.yml");

        if (!configFile.exists()) plugin.saveResource("config.yml", false);
        if (!storageFile.exists()) plugin.saveResource("storage.yml", false);

        config = YamlConfiguration.loadConfiguration(configFile);
        storageConfig = YamlConfiguration.loadConfiguration(storageFile);
    }

    public void reload() {
        load();
    }

    public FileConfiguration getConfig() { return config; }
    public FileConfiguration getStorageConfig() { return storageConfig; }

    public int getMaxPlayerUpdatesPerCycle() { return config.getInt("performance.max-player-updates-per-cycle", 100); }
    public long getPlaceholderCacheMs() { return config.getLong("performance.placeholder-cache-ms", 5000); }
    public int getLeaderboardRefreshSeconds() { return config.getInt("performance.leaderboard-refresh-seconds", 30); }
    public int getBatchSize() { return config.getInt("performance.batch-size", 50); }

    public boolean isCacheEnabled() { return config.getBoolean("cache.enabled", true); }
    public int getCacheRefreshInterval() { return config.getInt("cache.refresh-interval", 30); }
    public int getCacheMaxEntries() { return config.getInt("cache.max-entries", 100); }

    public LeaderboardType.RankingMode getRankingMode() {
        try {
            return LeaderboardType.RankingMode.valueOf(config.getString("ranking.mode", "COMPETITION").toUpperCase());
        } catch (IllegalArgumentException e) {
            return LeaderboardType.RankingMode.COMPETITION;
        }
    }

    public NumberFormatter.Mode getDefaultNumberFormat() {
        try {
            return NumberFormatter.Mode.valueOf(config.getString("number-format.mode", "SHORT").toUpperCase());
        } catch (IllegalArgumentException e) {
            return NumberFormatter.Mode.SHORT;
        }
    }

    public String getCurrencySymbol() { return config.getString("currency.symbol", "$"); }
    public String getCurrencyPosition() { return config.getString("currency.position", "BEFORE"); }

    public boolean isDebugLogging() { return config.getBoolean("logging.debug", false); }
    public boolean isDatabaseLogging() { return config.getBoolean("logging.database", false); }
    public boolean isPlaceholderLogging() { return config.getBoolean("logging.placeholder", false); }
    public boolean isGuiLogging() { return config.getBoolean("logging.gui", false); }

    public boolean isHeadsEnabled() { return config.getBoolean("heads.enabled", true); }
    public boolean isHeadTextureCacheEnabled() { return config.getBoolean("heads.cache-textures", true); }
}
