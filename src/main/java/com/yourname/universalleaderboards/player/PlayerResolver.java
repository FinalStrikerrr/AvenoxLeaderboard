package com.yourname.universalleaderboards.player;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thin wrapper around OfflinePlayer lookups with a small in-memory cache so repeated
 * GUI renders don't repeatedly hit Bukkit's player lookup tables.
 */
public class PlayerResolver {

    private final Map<UUID, OfflinePlayer> cache = new ConcurrentHashMap<>();

    public OfflinePlayer resolve(UUID uuid) {
        return cache.computeIfAbsent(uuid, Bukkit::getOfflinePlayer);
    }

    public void invalidate(UUID uuid) {
        cache.remove(uuid);
    }
}
