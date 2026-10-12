package com.yourname.universalleaderboards.leaderboard;

import com.yourname.universalleaderboards.leaderboard.LeaderboardType.Order;
import com.yourname.universalleaderboards.leaderboard.LeaderboardType.RankingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class LeaderboardSorter {

    public enum SortMode {
        HIGHEST_LOWEST("Highest + Lowest"),
        LOWEST_HIGHEST("Lowest + Highest"),
        A_Z("A + Z"),
        Z_A("Z + A"),
        ONLINE_ONLY("Online Only");

        private final String displayName;

        SortMode(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }

        public SortMode next() {
            SortMode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    private LeaderboardSorter() {}

    public static List<LeaderboardEntry> sort(List<LeaderboardEntry> entries, Order order) {
        List<LeaderboardEntry> copy = new ArrayList<>(entries);
        Comparator<LeaderboardEntry> comparator = Comparator.comparingDouble(LeaderboardEntry::getValue);
        if (order == Order.DESC) comparator = comparator.reversed();
        copy.sort(comparator);
        return copy;
    }


    /** Sorts a snapshot for the GUI without modifying the cached leaderboard snapshot. */
    public static List<LeaderboardEntry> sortForGui(List<LeaderboardEntry> entries, SortMode mode) {
        List<LeaderboardEntry> copy = new ArrayList<>(entries);
        if (mode == SortMode.ONLINE_ONLY) {
            copy.removeIf(entry -> org.bukkit.Bukkit.getPlayer(entry.getUuid()) == null);
            copy.sort(Comparator.comparingDouble(LeaderboardEntry::getValue).reversed());
            return copy;
        }
        switch (mode) {
            case LOWEST_HIGHEST -> copy.sort(Comparator.comparingDouble(LeaderboardEntry::getValue));
            case A_Z -> copy.sort(Comparator.comparing((LeaderboardEntry e) -> safeName(e.getDisplayName()), String.CASE_INSENSITIVE_ORDER));
            case Z_A -> copy.sort(Comparator.comparing((LeaderboardEntry e) -> safeName(e.getDisplayName()), String.CASE_INSENSITIVE_ORDER).reversed());
            case HIGHEST_LOWEST -> copy.sort(Comparator.comparingDouble(LeaderboardEntry::getValue).reversed());
            default -> {}
        }
        return copy;
    }

    private static String safeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    /**
     * Returns the 1-indexed rank of the entry at `index` within an already-sorted list,
     * according to the configured tie-handling mode.
     */
    public static int rankAt(List<LeaderboardEntry> sorted, int index, RankingMode mode) {
        if (index < 0 || index >= sorted.size()) return -1;
        double value = sorted.get(index).getValue();

        return switch (mode) {
            case ORDINAL -> index + 1;
            case DENSE -> {
                // Dense rank = 1 + number of distinct value changes among consecutive
                // entries from the top of the list down to (and including) this position.
                // Must compare each adjacent pair (i, i-1), not element i against a
                // "last changed value" seeded from element 0 itself - that seed made the
                // very first comparison trivially equal and silently skipped the boundary
                // between position 0 and position 1.
                int rank = 1;
                for (int i = 1; i <= index; i++) {
                    if (sorted.get(i).getValue() != sorted.get(i - 1).getValue()) {
                        rank++;
                    }
                }
                yield rank;
            }
            case COMPETITION -> {
                int rank = 1;
                for (int i = 0; i < index; i++) {
                    if (sorted.get(i).getValue() != value) rank++;
                }
                yield rank;
            }
        };
    }
}
