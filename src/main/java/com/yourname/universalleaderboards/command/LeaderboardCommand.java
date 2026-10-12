package com.yourname.universalleaderboards.command;

import com.yourname.universalleaderboards.config.MessageManager;
import com.yourname.universalleaderboards.gui.GUIManager;
import com.yourname.universalleaderboards.leaderboard.Leaderboard;
import com.yourname.universalleaderboards.leaderboard.LeaderboardManager;
import com.yourname.universalleaderboards.leaderboard.LeaderboardType;
import com.yourname.universalleaderboards.leaderboard.TimedLeaderboard;
import com.yourname.universalleaderboards.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public class LeaderboardCommand implements CommandExecutor {

    private final LeaderboardManager leaderboardManager;
    private final GUIManager guiManager;
    private final MessageManager messages;
    private final StorageProvider storage;
    private final LeaderboardAdminCommand adminCommand;

    public LeaderboardCommand(LeaderboardManager leaderboardManager, GUIManager guiManager, MessageManager messages,
                               StorageProvider storage, LeaderboardAdminCommand adminCommand) {
        this.leaderboardManager = leaderboardManager;
        this.guiManager = guiManager;
        this.messages = messages;
        this.storage = storage;
        this.adminCommand = adminCommand;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "player-only");
                return true;
            }
            if (!requireAnyPermission(sender, "universalleaderboards.use", "universalleaderboards.leaderboard")) return true;
            guiManager.openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "help" -> sendHelp(sender);
            case "open" -> handleOpen(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "reload" -> {
                if (requirePermission(sender, "universalleaderboards.admin.reload")) adminCommand.reload(sender);
            }
            case "reset" -> {
                if (requirePermission(sender, "universalleaderboards.admin.reset") && requireArgs(sender, args, 2)) {
                    adminCommand.resetBoard(sender, args[1]);
                }
            }
            case "resetall" -> {
                if (requirePermission(sender, "universalleaderboards.admin.reset")) adminCommand.resetAll(sender);
            }
            case "update" -> {
                if (requirePermission(sender, "universalleaderboards.admin.update") && requireArgs(sender, args, 2)) {
                    adminCommand.update(sender, args[1]);
                }
            }
            case "create" -> {
                if (requirePermission(sender, "universalleaderboards.admin.create")) handleCreate(sender, args);
            }
            case "delete" -> {
                if (requirePermission(sender, "universalleaderboards.admin.delete") && requireArgs(sender, args, 2)) {
                    adminCommand.delete(sender, args[1]);
                }
            }
            case "set" -> {
                if (requirePermission(sender, "universalleaderboards.admin") && requireArgs(sender, args, 4)) {
                    handleSet(sender, args);
                }
            }
            case "remove" -> {
                if (requirePermission(sender, "universalleaderboards.admin") && requireArgs(sender, args, 3)) {
                    adminCommand.removeValue(sender, args[1], args[2]);
                }
            }
            case "position" -> handlePosition(sender, args);
            case "search" -> handleSearch(sender, args);
            default -> sendHelp(sender);
        }
        return true;
    }

    private void handleOpen(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return;
        }
        if (!requireAnyPermission(sender, "universalleaderboards.use", "universalleaderboards.leaderboard")) return;
        if (args.length < 2) {
            guiManager.openMainMenu(player);
            return;
        }
        String boardId = args[1];
        if (leaderboardManager.getBoard(boardId).isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", boardId));
            return;
        }
        guiManager.openBoard(player, boardId, 0);
    }

    private void handleList(CommandSender sender) {
        if (leaderboardManager.getBoards().isEmpty()) {
            sender.sendMessage("No leaderboards configured.");
            return;
        }
        sender.sendMessage("§6Leaderboards:");
        for (Leaderboard board : leaderboardManager.getBoards()) {
            sender.sendMessage("§7 - §f" + board.getId() + " §7(" + board.getDisplayName() + "§7)");
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2)) return;
        Optional<Leaderboard> maybeBoard = leaderboardManager.getBoard(args[1]);
        if (maybeBoard.isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", args[1]));
            return;
        }
        Leaderboard board = maybeBoard.get();
        sender.sendMessage("§6Name: §f" + board.getDisplayName());
        sender.sendMessage("§6Placeholder: §f" + board.getPlaceholder());
        sender.sendMessage("§6Type: §f" + board.getValueType());
        sender.sendMessage("§6Order: §f" + board.getOrder());
        sender.sendMessage("§6Entries: §f" + board.getSnapshot().size() + "/" + board.getMaxEntries());
        sender.sendMessage("§6Update interval: §f" + board.getUpdateIntervalSeconds() + "s");
        sender.sendMessage("§6Enabled: §f" + board.isEnabled());
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "create-usage");
            return;
        }
        adminCommand.create(sender, args[1], args[2]);
    }

    private void handleSet(CommandSender sender, String[] args) {
        try {
            double value = Double.parseDouble(args[3]);
            adminCommand.setValue(sender, args[1], args[2], value);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid numeric value: " + args[3]);
        }
    }

    private void handlePosition(CommandSender sender, String[] args) {
        if (!requireArgs(sender, args, 2)) return;
        Optional<Leaderboard> maybeBoard = leaderboardManager.getBoard(args[1]);
        if (maybeBoard.isEmpty()) {
            messages.send(sender, "unknown-board", Map.of("%board%", args[1]));
            return;
        }
        Leaderboard board = maybeBoard.get();

        OfflinePlayer target;
        if (args.length >= 3) {
            target = Bukkit.getOfflinePlayer(args[2]);
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            messages.send(sender, "player-only");
            return;
        }

        String periodType = TimedLeaderboard.periodTypeKey(board.getTimedPeriod());
        storage.getRank(board.getId(), periodType, board.getPeriodStart(), target.getUniqueId(), board.getOrder() == LeaderboardType.Order.DESC)
                .thenCombine(storage.getEntry(board.getId(), periodType, board.getPeriodStart(), target.getUniqueId()), (rank, entry) -> {
                    if (rank <= 0 || entry == null) {
                        messages.send(sender, "no-position");
                    } else {
                        messages.send(sender, "position", Map.of(
                                "%board%", board.getDisplayName(),
                                "%rank%", String.valueOf(rank),
                                "%value%", board.formatValue(entry.getValue())
                        ));
                    }
                    return null;
                });
    }

    private void handleSearch(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messages.send(sender, "search-usage");
            return;
        }
        handlePosition(sender, new String[]{"position", args[1], args[2]});
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(messages.get("help-header"));
        sender.sendMessage("§e/leaderboard §7- open the main menu");
        sender.sendMessage("§e/leaderboard open <board> §7- open a specific leaderboard");
        sender.sendMessage("§e/leaderboard list §7- list all leaderboards");
        sender.sendMessage("§e/leaderboard info <board> §7- show board details");
        sender.sendMessage("§e/leaderboard position <board> [player] §7- show a rank");
        if (sender.hasPermission("universalleaderboards.admin")) {
            sender.sendMessage("§c/leaderboard create <id> <placeholder>");
            sender.sendMessage("§c/leaderboard delete <id>");
            sender.sendMessage("§c/leaderboard reset <board> §7/ §cresetall");
            sender.sendMessage("§c/leaderboard update <board>");
            sender.sendMessage("§c/leaderboard set <board> <player> <value>");
            sender.sendMessage("§c/leaderboard remove <board> <player>");
            sender.sendMessage("§c/leaderboard reload");
        }
    }

    private boolean requirePermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            messages.send(sender, "no-permission");
            return false;
        }
        return true;
    }

    private boolean requireAnyPermission(CommandSender sender, String... permissions) {
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) return true;
        }
        messages.send(sender, "no-permission");
        return false;
    }

    private boolean requireArgs(CommandSender sender, String[] args, int min) {
        if (args.length < min) {
            sender.sendMessage("§cMissing arguments. Use /leaderboard help.");
            return false;
        }
        return true;
    }
}
