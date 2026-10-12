package com.yourname.universalleaderboards.command;

import com.yourname.universalleaderboards.leaderboard.Leaderboard;
import com.yourname.universalleaderboards.leaderboard.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandCompleter implements TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "help", "open", "list", "info", "reload", "reset", "resetall", "update",
            "set", "remove", "position", "create", "delete", "edit", "search"
    );

    private final LeaderboardManager leaderboardManager;

    public CommandCompleter(LeaderboardManager leaderboardManager) {
        this.leaderboardManager = leaderboardManager;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(partial)) result.add(sub);
            }
            return result;
        }

        if (args.length == 2 && needsBoardArg(args[0])) {
            String partial = args[1].toLowerCase();
            for (Leaderboard board : leaderboardManager.getBoards()) {
                if (board.getId().toLowerCase().startsWith(partial)) result.add(board.getId());
            }
            return result;
        }

        if (args.length == 3 && needsPlayerArg(args[0])) {
            String partial = args[2].toLowerCase();
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(partial))
                    .collect(Collectors.toList());
        }

        return result;
    }

    private boolean needsBoardArg(String sub) {
        return switch (sub.toLowerCase()) {
            case "open", "info", "reset", "update", "set", "remove", "position", "delete", "edit", "search" -> true;
            default -> false;
        };
    }

    private boolean needsPlayerArg(String sub) {
        return switch (sub.toLowerCase()) {
            case "set", "remove", "position", "search" -> true;
            default -> false;
        };
    }
}
