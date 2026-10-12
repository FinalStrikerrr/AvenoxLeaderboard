package com.yourname.universalleaderboards.api.events;

import com.yourname.universalleaderboards.leaderboard.Leaderboard;
import com.yourname.universalleaderboards.leaderboard.LeaderboardEntry;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Container for the plugin's Bukkit events. Kept in one file for discoverability;
 * each is a standalone top-level-style class following normal Bukkit event conventions.
 */
public final class LeaderboardEvents {

    private LeaderboardEvents() {}

    public static class LeaderboardCreateEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Leaderboard leaderboard;
        public LeaderboardCreateEvent(Leaderboard leaderboard) { this.leaderboard = leaderboard; }
        public Leaderboard getLeaderboard() { return leaderboard; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static class LeaderboardDeleteEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final String boardId;
        public LeaderboardDeleteEvent(String boardId) { this.boardId = boardId; }
        public String getBoardId() { return boardId; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static class LeaderboardUpdateEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Leaderboard leaderboard;
        public LeaderboardUpdateEvent(Leaderboard leaderboard) { this.leaderboard = leaderboard; }
        public Leaderboard getLeaderboard() { return leaderboard; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static class LeaderboardResetEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Leaderboard leaderboard;
        private boolean cancelled;
        public LeaderboardResetEvent(Leaderboard leaderboard) { this.leaderboard = leaderboard; }
        public Leaderboard getLeaderboard() { return leaderboard; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static class LeaderboardEntryUpdateEvent extends Event {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Leaderboard leaderboard;
        private final LeaderboardEntry entry;
        public LeaderboardEntryUpdateEvent(Leaderboard leaderboard, LeaderboardEntry entry) {
            this.leaderboard = leaderboard;
            this.entry = entry;
        }
        public Leaderboard getLeaderboard() { return leaderboard; }
        public LeaderboardEntry getEntry() { return entry; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }

    public static class LeaderboardOpenEvent extends Event implements Cancellable {
        private static final HandlerList HANDLERS = new HandlerList();
        private final Player player;
        private final Leaderboard leaderboard;
        private boolean cancelled;
        public LeaderboardOpenEvent(Player player, Leaderboard leaderboard) {
            this.player = player;
            this.leaderboard = leaderboard;
        }
        public Player getPlayer() { return player; }
        public Leaderboard getLeaderboard() { return leaderboard; }
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
        @Override public HandlerList getHandlers() { return HANDLERS; }
        public static HandlerList getHandlerList() { return HANDLERS; }
    }
}
