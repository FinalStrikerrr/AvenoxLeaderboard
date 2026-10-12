package com.yourname.universalleaderboards.command;

import com.yourname.universalleaderboards.config.MessageManager;
import com.yourname.universalleaderboards.gui.GUIManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** /stats opens the sender's profile, while /stats <player> opens another player's profile. */
public class StatsCommand implements CommandExecutor {

    private final GUIManager guiManager;
    private final MessageManager messages;

    public StatsCommand(GUIManager guiManager, MessageManager messages) {
        this.guiManager = guiManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }
        if (!player.hasPermission("universalleaderboards.stats")) {
            messages.send(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            guiManager.openStats(player);
            return true;
        }

        if (args.length > 1) {
            messages.send(player, "stats-usage");
            return true;
        }

        if (!player.hasPermission("universalleaderboards.stats.others")) {
            messages.send(player, "no-permission");
            return true;
        }

        String targetName = args[0];
        Player online = Bukkit.getPlayerExact(targetName);
        OfflinePlayer target = online != null ? online : Bukkit.getOfflinePlayer(targetName);

        if (target.getUniqueId() == null || (target.getName() == null && online == null)) {
            messages.send(player, "stats-player-not-found", java.util.Map.of("%player%", targetName));
            return true;
        }

        guiManager.openStats(player, target);
        return true;
    }
}
