package com.yourname.universalleaderboards.config;

import com.yourname.universalleaderboards.leaderboard.Leaderboard;
import com.yourname.universalleaderboards.leaderboard.LeaderboardType.*;
import com.yourname.universalleaderboards.leaderboard.LeaderboardManager;
import com.yourname.universalleaderboards.util.NumberFormatter;
import org.bukkit.Statistic;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.logging.Logger;

public class LeaderboardConfigManager {

    private final Plugin plugin;
    private final Logger logger;
    private FileConfiguration leaderboardsConfig;
    private File leaderboardsFile;

    public LeaderboardConfigManager(Plugin plugin, Logger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }

    public void load() {
        leaderboardsFile = new File(plugin.getDataFolder(), "leaderboards.yml");
        if (!leaderboardsFile.exists()) plugin.saveResource("leaderboards.yml", false);
        leaderboardsConfig = YamlConfiguration.loadConfiguration(leaderboardsFile);
    }

    public void reload() { load(); }

    public void save() {
        try {
            leaderboardsConfig.save(leaderboardsFile);
        } catch (Exception e) {
            logger.warning("[AvenoxLeaderboard] Failed to save leaderboards.yml: " + e.getMessage());
        }
    }

    public FileConfiguration getRaw() { return leaderboardsConfig; }

    public void populate(LeaderboardManager manager) {
        manager.clearAll();

        ConfigurationSection boardsSection = leaderboardsConfig.getConfigurationSection("leaderboards");
        if (boardsSection != null) {
            for (String id : boardsSection.getKeys(false)) {
                ConfigurationSection section = boardsSection.getConfigurationSection(id);
                if (section == null) continue;
                try {
                    manager.registerBoard(parseBoard(id, section));
                } catch (Exception ex) {
                    logger.warning("[AvenoxLeaderboard] Failed to load leaderboard '" + id + "': " + ex.getMessage());
                }
            }
        }
    }

    private Leaderboard parseBoard(String id, ConfigurationSection section) {
        Leaderboard board = new Leaderboard(id);
        board.setDisplayName(section.getString("display-name", id));
        board.setPlaceholder(section.getString("placeholder", ""));
        board.setEnabled(section.getBoolean("enabled", true));
        board.setCategory(section.getString("category", "default"));
        board.setUpdateIntervalSeconds(section.getInt("update-interval", 30));
        board.setMaxEntries(section.getInt("max-entries", 100));
        board.setCustomProvider(section.getString("provider", null));
        board.setPermissionFilter(section.getString("permission", null));
        board.setWorldFilter(section.getStringList("worlds"));
        board.setExtraPlaceholders(section.getStringList("extras"));

        try {
            board.setValueType(ValueType.valueOf(section.getString("type", "NUMBER").toUpperCase()));
        } catch (IllegalArgumentException e) {
            board.setValueType(ValueType.NUMBER);
        }
        try {
            board.setOrder(Order.valueOf(section.getString("order", "DESC").toUpperCase()));
        } catch (IllegalArgumentException e) {
            board.setOrder(Order.DESC);
        }
        try {
            board.setTimedPeriod(TimedPeriod.valueOf(section.getString("timed", "ALL_TIME").toUpperCase()));
        } catch (IllegalArgumentException e) {
            board.setTimedPeriod(TimedPeriod.ALL_TIME);
        }
        board.setCustomIntervalSeconds(section.getInt("custom-interval-seconds", 0));
        if (board.getTimedPeriod() == TimedPeriod.CUSTOM && board.getCustomIntervalSeconds() <= 0) {
            logger.warning("[AvenoxLeaderboard] Board '" + id + "' is timed: CUSTOM but has no valid " +
                    "'custom-interval-seconds' set - it will fall back to a 60 second bucket.");
        }
        try {
            board.setScope(Scope.valueOf(section.getString("scope", "GLOBAL").toUpperCase()));
        } catch (IllegalArgumentException e) {
            board.setScope(Scope.GLOBAL);
        }

        // `source:` is intentionally a separate key from `type:` (which stays ValueType -
        // NUMBER/INTEGER/DECIMAL/TIME/BOOLEAN - unchanged). Omitting `source:` defaults to
        // PLACEHOLDER, preserving every pre-existing custom board's behavior exactly.
        try {
            board.setSource(SourceType.valueOf(section.getString("source", "PLACEHOLDER").toUpperCase()));
        } catch (IllegalArgumentException e) {
            logger.warning("[AvenoxLeaderboard] Board '" + id + "' has an invalid 'source' value; " +
                    "falling back to PLACEHOLDER.");
            board.setSource(SourceType.PLACEHOLDER);
        }

        if (board.getSource() == SourceType.STATISTIC) {
            String statName = section.getString("statistic", "");
            try {
                Statistic stat = Statistic.valueOf(statName.toUpperCase());
                if (stat.getType() != Statistic.Type.UNTYPED) {
                    // Validated once at load time rather than per-player at refresh time:
                    // a typed statistic (e.g. one requiring a Material or EntityType
                    // qualifier) would throw on every single player.getStatistic() call.
                    logger.warning("[AvenoxLeaderboard] Board '" + id + "' uses statistic '" + statName +
                            "', which requires a Material/EntityType qualifier and isn't supported as a " +
                            "simple STATISTIC board. Disabling this board.");
                    board.setEnabled(false);
                } else {
                    board.setStatistic(stat.name());
                    board.setStatisticDivisor(section.getDouble("divide-by", 1.0));
                }
            } catch (IllegalArgumentException e) {
                logger.warning("[AvenoxLeaderboard] Board '" + id + "' has an invalid 'statistic' value '" +
                        statName + "'. Disabling this board.");
                board.setEnabled(false);
            }
        }

        ConfigurationSection formatting = section.getConfigurationSection("formatting");
        if (formatting != null) {
            try {
                board.setFormatMode(NumberFormatter.Mode.valueOf(formatting.getString("mode", "SHORT").toUpperCase()));
            } catch (IllegalArgumentException e) {
                board.setFormatMode(NumberFormatter.Mode.SHORT);
            }
            board.setFormatPrefix(formatting.getString("prefix", ""));
            board.setFormatSuffix(formatting.getString("suffix", ""));
        }

        return board;
    }
}
