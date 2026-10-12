package com.yourname.universalleaderboards.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.Executor;
import java.util.logging.Logger;

public class StorageManager {

    private final Plugin plugin;
    private StorageProvider provider;
    private String typeInUse = "SQLITE";

    public StorageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public StorageProvider initialize(FileConfiguration storageConfig, Executor asyncExecutor, Logger logger) throws Exception {
        String type = storageConfig.getString("storage.type", "SQLITE").toUpperCase();
        typeInUse = type;

        if (type.equals("MYSQL")) {
            provider = new MySQLStorage(
                    storageConfig.getString("mysql.host", "localhost"),
                    storageConfig.getInt("mysql.port", 3306),
                    storageConfig.getString("mysql.database", "leaderboards"),
                    storageConfig.getString("mysql.username", "user"),
                    storageConfig.getString("mysql.password", "password"),
                    storageConfig.getInt("mysql.pool-size", 10),
                    storageConfig.getBoolean("mysql.ssl", false),
                    asyncExecutor, logger);
        } else {
            provider = new SQLiteStorage(plugin.getDataFolder(), asyncExecutor, logger);
        }

        try {
            provider.initialize();
        } catch (Exception e) {
            logger.severe("Failed to initialize " + type + " storage, falling back to SQLite: " + e.getMessage());
            typeInUse = "SQLITE (fallback)";
            provider = new SQLiteStorage(plugin.getDataFolder(), asyncExecutor, logger);
            provider.initialize();
        }

        return provider;
    }

    public StorageProvider getProvider() {
        return provider;
    }

    public String getTypeInUse() {
        return typeInUse;
    }

    public void close() {
        if (provider != null) provider.close();
    }
}
