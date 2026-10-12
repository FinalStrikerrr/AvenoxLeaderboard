package com.yourname.universalleaderboards.placeholder;

import com.yourname.universalleaderboards.api.CustomLeaderboardProvider;
import com.yourname.universalleaderboards.cache.LeaderboardCache;
import com.yourname.universalleaderboards.hook.MoneyProvider;
import com.yourname.universalleaderboards.leaderboard.Leaderboard;
import com.yourname.universalleaderboards.leaderboard.LeaderboardEntry;
import com.yourname.universalleaderboards.leaderboard.LeaderboardType.SourceType;
import com.yourname.universalleaderboards.scheduler.SchedulerAdapter;
import com.yourname.universalleaderboards.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Resolves values for a leaderboard's tracked players. Runs entirely off the main thread
 * when invoked from LeaderboardUpdater's async task. Respects max-player-updates-per-cycle
 * so a huge player base is spread across multiple cycles rather than causing a spike.
 *
 * COUNTER-source boards (blocks-mined, blocks-placed, mob-kills) are never resolved here at
 * all - LeaderboardUpdater skips straight to re-reading the stored snapshot for those, since
 * their values are maintained directly by PersistentCounterService's atomic DB increments.
 */
public class PlaceholderManager {

    private final PlaceholderResolver resolver;
    private final LeaderboardCache cache;
    private final MoneyProvider moneyProvider;
    private final Logger logger;
    private final int maxUpdatesPerCycle;
    private final SchedulerAdapter scheduler;
    private final Map<String, CustomLeaderboardProvider> customProviders = new ConcurrentHashMap<>();
    private final Set<UUID> excludedPermissionCache = ConcurrentHashMap.newKeySet();

    public PlaceholderManager(PlaceholderResolver resolver, LeaderboardCache cache, MoneyProvider moneyProvider,
                               Logger logger, int maxUpdatesPerCycle, SchedulerAdapter scheduler) {
        this.resolver = resolver;
        this.cache = cache;
        this.moneyProvider = moneyProvider;
        this.logger = logger;
        this.maxUpdatesPerCycle = maxUpdatesPerCycle;
        this.scheduler = scheduler;
    }

    public void registerProvider(String id, CustomLeaderboardProvider provider) {
        customProviders.put(id.toLowerCase(Locale.ROOT), provider);
    }

    public void unregisterProvider(String id) {
        customProviders.remove(id.toLowerCase(Locale.ROOT));
    }

    /**
     * Resolves values for currently-online players tracked by this board and returns
     * the newly built entries. Offline players already in storage are left untouched
     * (their prior entry remains valid until they log in again).
     */
    public List<LeaderboardEntry> resolveOnlinePlayers(Leaderboard board) {
        if (!Bukkit.isPrimaryThread()) {
            CompletableFuture<List<LeaderboardEntry>> future = new CompletableFuture<>();
            scheduler.runGlobal(() -> {
                try {
                    future.complete(resolveOnlinePlayersOnGlobalThread(board));
                } catch (Throwable ex) {
                    logger.warning("[AvenoxLeaderboard] Failed to resolve board '" + board.getId() + "': " + ex.getMessage());
                    future.complete(List.of());
                }
            });
            return future.join();
        }
        return resolveOnlinePlayersOnGlobalThread(board);
    }

    private List<LeaderboardEntry> resolveOnlinePlayersOnGlobalThread(Leaderboard board) {
        List<LeaderboardEntry> results = new ArrayList<>();
        int processed = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (processed >= maxUpdatesPerCycle) break;
            if (player.hasPermission("universalleaderboards.exclude")) continue;
            if (!worldAllowed(board, player)) continue;
            if (board.getPermissionFilter() != null && !board.getPermissionFilter().isEmpty()
                    && !player.hasPermission(board.getPermissionFilter())) continue;

            Double value = resolveValue(board, player);
            processed++;
            if (value == null) continue;

            results.add(new LeaderboardEntry(
                    player.getUniqueId(),
                    player.getName(),
                    player.getDisplayName(),
                    value,
                    String.valueOf(value),
                    System.currentTimeMillis()
            ));
        }
        return results;
    }

    private boolean worldAllowed(Leaderboard board, Player player) {
        List<String> worlds = board.getWorldFilter();
        if (worlds == null || worlds.isEmpty()) return true;
        return worlds.contains(player.getWorld().getName());
    }

    private Double resolveValue(Leaderboard board, OfflinePlayer player) {
        SourceType source = board.getSource();

        if (source == SourceType.STATISTIC) {
            // Config load already validated the statistic name exists and is UNTYPED
            // (see LeaderboardConfigManager), so this should never throw in practice - the
            // try/catch is a last-resort safety net, not the primary validation path.
            try {
                Statistic stat = Statistic.valueOf(board.getStatistic());
                return (double) player.getStatistic(stat) / board.getStatisticDivisor();
            } catch (Exception ex) {
                logger.warning("[AvenoxLeaderboard] Failed to read statistic '" + board.getStatistic() +
                        "' for board '" + board.getId() + "': " + ex.getMessage());
                return null;
            }
        }

        if (source == SourceType.VAULT) {
            if (moneyProvider == null || !moneyProvider.isAvailable()) return null;
            // Already running on an async task here (LeaderboardUpdater's refresh cycle),
            // so blocking on the balance future is safe - it never touches the main thread.
            return moneyProvider.getBalanceSync(player.getUniqueId());
        }

        String customProviderId = board.getCustomProvider();
        if (customProviderId != null && !customProviderId.isEmpty()) {
            CustomLeaderboardProvider provider = customProviders.get(customProviderId.toLowerCase(Locale.ROOT));
            if (provider == null) return null;
            try {
                return provider.getValue(player.getUniqueId());
            } catch (Exception ex) {
                logger.warning("[AvenoxLeaderboard] Custom provider '" + customProviderId + "' threw an exception: " + ex.getMessage());
                return null;
            }
        }

        String cached = cache.getCachedValue(board.getId(), player.getUniqueId());
        String raw = cached != null ? cached : resolver.resolve(player, board.getPlaceholder());
        if (raw == null) return null;
        if (cached == null) cache.cacheValue(board.getId(), player.getUniqueId(), raw);

        return NumberFormatter.parse(raw);
    }
}
