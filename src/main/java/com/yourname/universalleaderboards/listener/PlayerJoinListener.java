package com.yourname.universalleaderboards.listener;

import com.yourname.universalleaderboards.leaderboard.LeaderboardManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/** Tracks every joining player on every enabled leaderboard, including zero-value entries. */
public final class PlayerJoinListener implements Listener {

    private final LeaderboardManager leaderboardManager;

    public PlayerJoinListener(LeaderboardManager leaderboardManager) {
        this.leaderboardManager = leaderboardManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        leaderboardManager.trackPlayerOnJoin(event.getPlayer());
    }
}
