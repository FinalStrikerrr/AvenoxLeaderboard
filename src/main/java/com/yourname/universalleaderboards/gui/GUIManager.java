package com.yourname.universalleaderboards.gui;

import com.yourname.universalleaderboards.config.GuiConfigManager;
import com.yourname.universalleaderboards.config.MessageManager;
import com.yourname.universalleaderboards.leaderboard.*;
import com.yourname.universalleaderboards.scheduler.SchedulerAdapter;
import com.yourname.universalleaderboards.stats.StatsManager;
import com.yourname.universalleaderboards.stats.PersistentCounterService;
import com.yourname.universalleaderboards.storage.StorageProvider;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Single listener for all plugin GUIs. Section 40 (GUI safety) requirements are enforced here:
 * every click in one of our inventories is cancelled by default; only clicks that land on a
 * slot we explicitly registered as a GUIItem trigger an action. Player inventory clicks while
 * a plugin GUI is open are also cancelled to prevent shift-click/drag/hotbar-swap exploits
 * moving items into or out of the read-only leaderboard inventory.
 *
 * THREADING: openMainMenu/openBoard call Player#openInventory synchronously with no async
 * gap beforehand, so they're always safe regardless of which thread invokes them. openStats,
 * showMyPosition, and showProfile all do async storage/money lookups first - their
 * continuations MUST be dispatched back onto the player's own thread via
 * SchedulerAdapter#runAtEntity before touching any player-facing API (openInventory,
 * sendMessage, playSound). This was a real bug: the previous version called
 * player.openInventory(...) directly inside a CompletableFuture#thenAccept callback, which
 * runs on whichever background thread happened to complete the future - not the main/entity
 * thread. Paper/Folia can silently reject or misbehave on an off-thread inventory open, with
 * no visible error, which is exactly why /stats appeared to simply "not work."
 */
public class GUIManager implements Listener {

    private final LeaderboardGUI renderer;
    private final LeaderboardManager leaderboardManager;
    private final GuiConfigManager guiConfig;
    private final MessageManager messages;
    private final StorageProvider storage;
    private final StatsManager statsManager;
    private final SchedulerAdapter scheduler;
    private final Logger logger;
    private final PersistentCounterService counterService;

    public GUIManager(LeaderboardGUI renderer, LeaderboardManager leaderboardManager,
                       GuiConfigManager guiConfig, MessageManager messages, StorageProvider storage,
                       StatsManager statsManager, SchedulerAdapter scheduler, Logger logger, PersistentCounterService counterService) {
        this.renderer = renderer;
        this.leaderboardManager = leaderboardManager;
        this.guiConfig = guiConfig;
        this.messages = messages;
        this.storage = storage;
        this.statsManager = statsManager;
        this.scheduler = scheduler;
        this.logger = logger;
        this.counterService = counterService;
    }

    public void openMainMenu(Player player) {
        GUIHolder holder = renderer.buildMainMenu();
        player.openInventory(holder.getInventory());
        playSound(player, "open");
    }

    public void openBoard(Player player, String boardId, int page) {
        openBoard(player, boardId, page, null);
    }

    private void openBoard(Player player, String boardId, int page, LeaderboardSorter.SortMode requestedSort) {
        Optional<Leaderboard> maybeBoard = leaderboardManager.getBoard(boardId);
        if (maybeBoard.isEmpty()) return;
        Leaderboard board = maybeBoard.get();

        Runnable refreshAndOpen = () -> leaderboardManager.forceRefresh(board, () ->
                scheduler.runAtEntity(player, () -> {
                    if (!player.isOnline()) return;
                    GUIHolder holder = renderer.buildBoardView(player, board, page, requestedSort == null ?
                            (board.getOrder() == LeaderboardType.Order.ASC ? LeaderboardSorter.SortMode.LOWEST_HIGHEST : LeaderboardSorter.SortMode.HIGHEST_LOWEST)
                            : requestedSort);
                    player.openInventory(holder.getInventory());
                    playSound(player, "open");
                })
        );

        if (board.getSource() == LeaderboardType.SourceType.COUNTER && counterService != null) {
            counterService.flush().thenRun(refreshAndOpen);
        } else {
            refreshAndOpen.run();
        }
    }

    /** Opens the invoking player's own profile. */
    public void openStats(Player viewer) {
        openStats(viewer, viewer);
    }

    /** Opens the profile of another player while keeping the viewer in the GUI. */
    public void openStats(Player viewer, org.bukkit.OfflinePlayer target) {
        if (viewer == null || target == null) return;

        Player onlineTarget = target.getPlayer();
        java.util.UUID targetUuid = target.getUniqueId();
        statsManager.getStats(targetUuid, onlineTarget)
                .thenAccept(stats -> scheduler.runAtEntity(viewer, () -> {
                    if (!viewer.isOnline()) return;
                    GUIHolder holder = renderer.buildStatsView(viewer, target, stats);
                    viewer.openInventory(holder.getInventory());
                    playSound(viewer, "open");
                }))
                .exceptionally(ex -> {
                    logger.warning("[AvenoxLeaderboard] Failed to load stats for " + targetUuid + ": " + ex.getMessage());
                    scheduler.runAtEntity(viewer, () -> {
                        if (viewer.isOnline()) messages.send(viewer, "database-error");
                    });
                    return null;
                });
    }

    @EventHandler(ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder rawHolder = event.getInventory().getHolder();
        if (!(rawHolder instanceof GUIHolder holder)) return;

        // Cancel everything by default - GUI safety requirement (section 40).
        event.setCancelled(true);

        // Ignore clicks in the player's own inventory while a GUI is open, and ignore
        // shift-click/number-key/double-click attempts entirely; only a plain click on a
        // registered slot in the TOP inventory does anything.
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) return;

        GUIPage page = holder.getRenderedPage();
        if (page == null) return;
        GUIItem clicked = page.get(event.getRawSlot());
        if (clicked == null) return;

        handleAction(player, holder, clicked);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GUIHolder) {
            event.setCancelled(true);
        }
    }

    private void handleAction(Player player, GUIHolder holder, GUIItem item) {
        switch (item.getAction()) {
            case OPEN_MENU -> openMainMenu(player);
            case OPEN_BOARD -> openBoard(player, item.getActionData(), 0);
            case NEXT_PAGE -> openBoard(player, holder.getBoardId(), holder.getPage() + 1, holder.getSortMode());
            case PREVIOUS_PAGE -> openBoard(player, holder.getBoardId(), Math.max(0, holder.getPage() - 1), holder.getSortMode());
            case SORTING -> openBoard(player, holder.getBoardId(), 0, holder.getSortMode().next());
            case CLOSE -> {
                playSound(player, "close");
                player.closeInventory();
            }
            case PLAYER_PROFILE -> openStatsForUuid(player, item.getActionData());
            case OPEN_STATS -> openStats(player);
            case MESSAGE -> handleMessageAction(player, item.getActionData());
            case COMMAND -> {
                if (item.getActionData() != null) player.performCommand(item.getActionData());
            }
            case NONE -> {}
        }
        if (item.getAction() == GUIItem.ClickAction.NEXT_PAGE || item.getAction() == GUIItem.ClickAction.PREVIOUS_PAGE || item.getAction() == GUIItem.ClickAction.SORTING) {
            playSound(player, "page");
        } else if (item.getAction() != GUIItem.ClickAction.CLOSE) {
            playSound(player, "click");
        }
    }

    private void handleMessageAction(Player player, String data) {
        if (data == null) return;
        if (data.startsWith("my-position:")) {
            String boardId = data.substring("my-position:".length());
            showMyPosition(player, boardId);
        }
    }

    private void showMyPosition(Player player, String boardId) {
        Optional<Leaderboard> maybeBoard = leaderboardManager.getBoard(boardId);
        if (maybeBoard.isEmpty()) return;
        Leaderboard board = maybeBoard.get();
        String periodType = TimedLeaderboard.periodTypeKey(board.getTimedPeriod());

        storage.getRank(board.getId(), periodType, board.getPeriodStart(), player.getUniqueId(), board.getOrder() == LeaderboardType.Order.DESC)
                .thenCombine(storage.getEntry(board.getId(), periodType, board.getPeriodStart(), player.getUniqueId()), (rank, entry) -> {
                    scheduler.runAtEntity(player, () -> {
                        if (!player.isOnline()) return;
                        if (rank <= 0 || entry == null) {
                            messages.send(player, "no-position");
                        } else {
                            messages.send(player, "position", Map.of(
                                    "%board%", board.getDisplayName(),
                                    "%rank%", String.valueOf(rank),
                                    "%value%", board.formatValue(entry.getValue())
                            ));
                        }
                    });
                    return null;
                })
                .exceptionally(ex -> {
                    logger.warning("[AvenoxLeaderboard] Failed to resolve position for " + player.getName() + " on board '" + boardId + "': " + ex.getMessage());
                    return null;
                });
    }

    private void openStatsForUuid(Player viewer, String uuidString) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException ignored) {
            return;
        }

        org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(uuid);
        openStats(viewer, target);
    }

    private void playSound(Player player, String key) {
        String soundName = guiConfig.getSound(key);
        if (soundName == null) return;
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()), 1f, 1f);
        } catch (IllegalArgumentException ignored) {
        }
    }
}
