package com.yourname.universalleaderboards.placeholder;

import com.yourname.universalleaderboards.leaderboard.Leaderboard;
import com.yourname.universalleaderboards.leaderboard.LeaderboardEntry;
import com.yourname.universalleaderboards.leaderboard.LeaderboardManager;
import com.yourname.universalleaderboards.leaderboard.LeaderboardSorter;
import com.yourname.universalleaderboards.leaderboard.LeaderboardType;
import com.yourname.universalleaderboards.storage.StorageProvider;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Optional;

/**
 * Registers the plugin's own PlaceholderAPI expansion so external systems (holograms,
 * scoreboards, other GUIs) can read leaderboard data via placeholders like:
 *
 *   %ulb_<board>_<position>_name%
 *   %ulb_<board>_<position>_value%
 *   %ulb_<board>_<position>_displayname%
 *   %ulb_<board>_<position>_position%
 *   %ulb_<board>_position%      (current viewer's rank)
 *   %ulb_<board>_value%         (current viewer's value)
 *   %ulb_<board>_size%          (total tracked entries, cached snapshot size)
 *   %ulb_<board>_total%         (alias of size)
 *
 * The '*' separator variants from the spec are supported as aliases since '*' is not a
 * valid character inside a PlaceholderAPI placeholder body; both underscore and asterisk
 * forms resolve identically here by normalizing '*' to '_' before parsing.
 */
public class UniversalLeaderboardsExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final LeaderboardManager leaderboardManager;
    private final StorageProvider storage;

    public UniversalLeaderboardsExpansion(Plugin plugin, LeaderboardManager leaderboardManager, StorageProvider storage) {
        this.plugin = plugin;
        this.leaderboardManager = leaderboardManager;
        this.storage = storage;
    }

    @Override
    public String getIdentifier() {
        return "ulb";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String normalized = params.replace('*', '_');
        String[] parts = normalized.split("_");
        if (parts.length < 2) return "";

        String boardId = parts[0];
        Optional<Leaderboard> maybeBoard = leaderboardManager.getBoard(boardId);
        if (maybeBoard.isEmpty()) return "";
        Leaderboard board = maybeBoard.get();

        // %ulb_<board>_size% or %ulb_<board>_total%
        if (parts.length == 2 && (parts[1].equalsIgnoreCase("size") || parts[1].equalsIgnoreCase("total"))) {
            return String.valueOf(board.getSnapshot().size());
        }

        // %ulb_<board>_position% or %ulb_<board>_value% - current viewer
        if (parts.length == 2 && player != null) {
            if (parts[1].equalsIgnoreCase("position")) {
                LeaderboardEntry entry = findInSnapshot(board, player.getUniqueId());
                if (entry == null) return "N/A";
                int index = board.getSnapshot().indexOf(entry);
                return String.valueOf(LeaderboardSorter.rankAt(board.getSnapshot(), index, LeaderboardType.RankingMode.COMPETITION));
            }
            if (parts[1].equalsIgnoreCase("value")) {
                LeaderboardEntry entry = findInSnapshot(board, player.getUniqueId());
                return entry == null ? "N/A" : board.formatValue(entry.getValue());
            }
        }

        // %ulb_<board>_<position>_name|value|displayname|position%
        if (parts.length >= 3) {
            int position;
            try {
                position = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return "";
            }
            List<LeaderboardEntry> snapshot = board.getSnapshot();
            if (position < 1 || position > snapshot.size()) return "";
            LeaderboardEntry entry = snapshot.get(position - 1);

            String field = parts[2];
            return switch (field.toLowerCase()) {
                case "name" -> entry.getUsername() == null ? "Unknown" : entry.getUsername();
                case "displayname" -> entry.getDisplayName();
                case "value" -> board.formatValue(entry.getValue());
                case "position" -> String.valueOf(position);
                default -> "";
            };
        }

        return "";
    }

    private LeaderboardEntry findInSnapshot(Leaderboard board, java.util.UUID uuid) {
        for (LeaderboardEntry entry : board.getSnapshot()) {
            if (entry.getUuid().equals(uuid)) return entry;
        }
        return null;
    }
}
