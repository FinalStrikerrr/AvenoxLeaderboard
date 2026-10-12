package com.yourname.universalleaderboards.config;

import com.yourname.universalleaderboards.util.ColorUtil;
import com.yourname.universalleaderboards.util.TextUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Map;

public class MessageManager {

    private final Plugin plugin;
    private FileConfiguration messages;
    private File messagesFile;

    public MessageManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void reload() {
        load();
    }

    private String prefix() {
        return ColorUtil.colorize(messages.getString("prefix", ""));
    }

    public String get(String key) {
        String raw = messages.getString(key, key);
        return ColorUtil.colorize(raw);
    }

    public String get(String key, Map<String, String> replacements) {
        String raw = messages.getString(key, key);
        return ColorUtil.colorize(TextUtil.replace(raw, replacements));
    }

    public void send(CommandSender sender, String key) {
        sender.sendMessage(prefix() + get(key));
    }

    public void send(CommandSender sender, String key, Map<String, String> replacements) {
        sender.sendMessage(prefix() + get(key, replacements));
    }
}
