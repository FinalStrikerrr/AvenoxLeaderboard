package com.yourname.universalleaderboards.stats;

import com.yourname.universalleaderboards.compat.VersionAdapter;
import com.yourname.universalleaderboards.hook.MoneyProvider;
import com.yourname.universalleaderboards.leaderboard.LeaderboardEntry;
import com.yourname.universalleaderboards.listener.BlockBreakListener;
import com.yourname.universalleaderboards.listener.BlockPlaceListener;
import com.yourname.universalleaderboards.listener.MobKillListener;
import com.yourname.universalleaderboards.storage.StorageProvider;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Loads personal statistics and, when requested, another player's statistics.
 *
 * Online players use live Bukkit statistics for kills/deaths/playtime and the current
 * economy provider for balance. Offline/other-player views use the persistent leaderboard
 * rows, which means a player can be inspected by UUID even when they are not online.
 */
public class StatsManager {

    private static final String PERIOD_TYPE = "ALL_TIME";
    private static final long PERIOD_START = 0L;

    private final StorageProvider storage;
    private final MoneyProvider moneyProvider;

    public StatsManager(StorageProvider storage, MoneyProvider moneyProvider) {
        this.storage = storage;
        this.moneyProvider = moneyProvider;
    }

    /** Live, point-in-time stats for an online player. */
    public CompletableFuture<PlayerStats> getStats(Player player) {
        long kills = player.getStatistic(Statistic.PLAYER_KILLS);
        long deaths = player.getStatistic(Statistic.DEATHS);
        long playtimeSeconds = VersionAdapter.getPlaytimeTicks(player) / 20L;

        CompletableFuture<LeaderboardEntry> blocksMinedFuture =
                storage.getEntry(BlockBreakListener.BOARD_ID, PERIOD_TYPE, PERIOD_START, player.getUniqueId());
        CompletableFuture<LeaderboardEntry> blocksPlacedFuture =
                storage.getEntry(BlockPlaceListener.BOARD_ID, PERIOD_TYPE, PERIOD_START, player.getUniqueId());
        CompletableFuture<LeaderboardEntry> mobKillsFuture =
                storage.getEntry(MobKillListener.BOARD_ID, PERIOD_TYPE, PERIOD_START, player.getUniqueId());
        CompletableFuture<Double> moneyFuture = moneyProvider.getBalance(player.getUniqueId());

        return CompletableFuture.allOf(blocksMinedFuture, blocksPlacedFuture, mobKillsFuture, moneyFuture)
                .thenApply(v -> new PlayerStats(
                        kills,
                        deaths,
                        moneyFuture.join(),
                        valueOrZero(blocksMinedFuture.join()),
                        valueOrZero(blocksPlacedFuture.join()),
                        playtimeSeconds,
                        valueOrZero(mobKillsFuture.join())
                ));
    }

    /**
     * Persistent stats for any UUID. This is used by leaderboard player-profile clicks and
     * /stats <player>. Missing rows intentionally become zero so every tracked player has a
     * complete, predictable profile instead of an empty GUI.
     */
    public CompletableFuture<PlayerStats> getStats(UUID uuid) {
        CompletableFuture<LeaderboardEntry> killsFuture = storage.getEntry("kills", PERIOD_TYPE, PERIOD_START, uuid);
        CompletableFuture<LeaderboardEntry> deathsFuture = storage.getEntry("deaths", PERIOD_TYPE, PERIOD_START, uuid);
        CompletableFuture<LeaderboardEntry> moneyFuture = storage.getEntry("money", PERIOD_TYPE, PERIOD_START, uuid);
        CompletableFuture<LeaderboardEntry> blocksMinedFuture = storage.getEntry(BlockBreakListener.BOARD_ID, PERIOD_TYPE, PERIOD_START, uuid);
        CompletableFuture<LeaderboardEntry> blocksPlacedFuture = storage.getEntry(BlockPlaceListener.BOARD_ID, PERIOD_TYPE, PERIOD_START, uuid);
        CompletableFuture<LeaderboardEntry> playtimeFuture = storage.getEntry("playtime", PERIOD_TYPE, PERIOD_START, uuid);
        CompletableFuture<LeaderboardEntry> mobKillsFuture = storage.getEntry(MobKillListener.BOARD_ID, PERIOD_TYPE, PERIOD_START, uuid);

        return CompletableFuture.allOf(
                killsFuture, deathsFuture, moneyFuture, blocksMinedFuture,
                blocksPlacedFuture, playtimeFuture, mobKillsFuture
        ).thenApply(v -> new PlayerStats(
                valueOrZero(killsFuture.join()),
                valueOrZero(deathsFuture.join()),
                moneyFuture.join() == null ? null : moneyFuture.join().getValue(),
                valueOrZero(blocksMinedFuture.join()),
                valueOrZero(blocksPlacedFuture.join()),
                valueOrZero(playtimeFuture.join()),
                valueOrZero(mobKillsFuture.join())
        ));
    }

    /** Uses live values when the requested target is online, otherwise persistent values. */
    public CompletableFuture<PlayerStats> getStats(UUID uuid, Player onlineTarget) {
        if (onlineTarget != null && onlineTarget.getUniqueId().equals(uuid)) {
            return getStats(onlineTarget);
        }
        return getStats(uuid);
    }

    private long valueOrZero(LeaderboardEntry entry) {
        return entry == null ? 0L : (long) entry.getValue();
    }
}
