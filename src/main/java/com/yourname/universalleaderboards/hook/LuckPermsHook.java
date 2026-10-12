package com.yourname.universalleaderboards.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Soft dependency. In practice prefix/suffix display is handled through PlaceholderAPI's
 * LuckPerms expansion (%luckperms_prefix%) referenced via a board's "extras" list, so this
 * hook mainly exists to confirm LuckPerms presence for the startup summary and for future
 * direct-API integrations that don't need to round-trip through PlaceholderAPI.
 */
public class LuckPermsHook {

    private boolean enabled = false;

    public boolean setup(Plugin plugin) {
        enabled = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        return enabled;
    }

    public boolean isEnabled() { return enabled; }
}
