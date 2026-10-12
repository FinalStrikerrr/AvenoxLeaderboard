package com.yourname.universalleaderboards.command;

import com.yourname.universalleaderboards.config.ConfigManager;
import com.yourname.universalleaderboards.config.LeaderboardConfigManager;
import com.yourname.universalleaderboards.config.MessageManager;
import com.yourname.universalleaderboards.leaderboard.*;
import com.yourname.universalleaderboards.placeholder.PlaceholderResolver;
import com.yourname.universalleaderboards.storage.StorageProvider;
import com.yourname.universalleaderboards.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class LeaderboardAdminCommand {

    private final LeaderboardManager leaderboardManager;
    private final LeaderboardConfigManager configManager;
    private final MessageManager messages;
    private final StorageProvider storage;
    private final PlaceholderResolver resolver;
    private final Runnable reloadCallback;

    public LeaderboardAdminCommand(LeaderboardManager leaderboardManager, LeaderboardConfigManager configManager,
                                    MessageManager messages, StorageProvider storage, PlaceholderResolver resolver,
                                    Runnable reloadCallback) {
        this.leaderboardManager = leaderboardManager;
        this.configManager = configManager;
        this.messages = messages;
        this.storage = storage;
        this.resolver = resolver;
        this.reloadCallback = reloadCallback;
    }

    public void reload(CommandSender sender) {
        reloadCallback.run();
        messages.send(sender, "reload");
    }

    public void resetBoard(CommandSender sender, String boardId) {
        Optional<Leaderboard> board = leaderboardManager.getBoard(boardId);
        if (board.isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", boardId));
            return;
        }
        leaderboardManager.resetBoard(board.get(), () ->
                messages.send(sender, "reset", Map.of("%board%", boardId)));
    }

    public void resetAll(CommandSender sender) {
        leaderboardManager.resetAll(() -> messages.send(sender, "resetall"));
    }

    public void update(CommandSender sender, String boardId) {
        Optional<Leaderboard> board = leaderboardManager.getBoard(boardId);
        if (board.isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", boardId));
            return;
        }
        messages.send(sender, "updating", Map.of("%board%", boardId));
        leaderboardManager.forceRefresh(board.get());
        messages.send(sender, "updated", Map.of("%board%", boardId));
    }

    public void create(CommandSender sender, String id, String placeholder) {
        if (leaderboardManager.getBoard(id).isPresent()) {
            sender.sendMessage("A leaderboard with that id already exists.");
            return;
        }

        ConfigurationSection section = configManager.getRaw()
                .getConfigurationSection("leaderboards");
        if (section == null) {
            section = configManager.getRaw().createSection("leaderboards");
        }
        ConfigurationSection boardSection = section.createSection(id);
        boardSection.set("display-name", "&f" + id);
        boardSection.set("placeholder", placeholder);
        boardSection.set("type", "NUMBER");
        boardSection.set("order", "DESC");
        boardSection.set("timed", "ALL_TIME");
        boardSection.set("update-interval", 30);
        boardSection.set("max-entries", 100);
        boardSection.set("category", "default");
        boardSection.set("enabled", true);
        configManager.save();
        configManager.populate(leaderboardManager);

        // Validate the placeholder against the command sender if they're a player, per
        // section 29 - warn, but never crash or block creation.
        if (sender instanceof org.bukkit.entity.Player player) {
            String raw = resolver.resolve(player, placeholder);
            if (raw == null || NumberFormatter.parse(raw) == null) {
                messages.send(sender, "invalid-placeholder", Map.of("%placeholder%", placeholder));
            }
        }

        messages.send(sender, "created", Map.of("%board%", id, "%placeholder%", placeholder));
    }

    public void delete(CommandSender sender, String id) {
        Optional<Leaderboard> board = leaderboardManager.getBoard(id);
        if (board.isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", id));
            return;
        }
        ConfigurationSection section = configManager.getRaw().getConfigurationSection("leaderboards");
        if (section != null) section.set(id, null);
        configManager.save();
        leaderboardManager.unregisterBoard(id);
        messages.send(sender, "deleted", Map.of("%board%", id));
    }

    public void setValue(CommandSender sender, String boardId, String playerName, double value) {
        Optional<Leaderboard> maybeBoard = leaderboardManager.getBoard(boardId);
        if (maybeBoard.isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", boardId));
            return;
        }
        Leaderboard board = maybeBoard.get();
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        LeaderboardEntry entry = new LeaderboardEntry(target.getUniqueId(), target.getName(), target.getName(),
                value, String.valueOf(value), System.currentTimeMillis());

        String periodType = TimedLeaderboard.periodTypeKey(board.getTimedPeriod());
        storage.upsertEntry(board.getId(), periodType, board.getPeriodStart(), entry)
                .thenRun(() -> leaderboardManager.forceRefresh(board));

        messages.send(sender, "value-set", Map.of("%player%", playerName, "%board%", boardId, "%value%", String.valueOf(value)));
    }

    public void removeValue(CommandSender sender, String boardId, String playerName) {
        Optional<Leaderboard> maybeBoard = leaderboardManager.getBoard(boardId);
        if (maybeBoard.isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", boardId));
            return;
        }
        Leaderboard board = maybeBoard.get();
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        String periodType = TimedLeaderboard.periodTypeKey(board.getTimedPeriod());
        storage.removeEntry(board.getId(), periodType, board.getPeriodStart(), target.getUniqueId())
                .thenRun(() -> leaderboardManager.forceRefresh(board));

        messages.send(sender, "value-removed", Map.of("%player%", playerName, "%board%", boardId));
    }
}
