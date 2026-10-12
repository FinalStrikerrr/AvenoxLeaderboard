package com.yourname.universalleaderboards.leaderboard;

import com.yourname.universalleaderboards.leaderboard.LeaderboardType.Order;
import com.yourname.universalleaderboards.leaderboard.LeaderboardType.RankingMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardSorterTest {

    private LeaderboardEntry entry(double value) {
        return new LeaderboardEntry(UUID.randomUUID(), "Player", "Player", value, String.valueOf(value), 0L);
    }

    @Test
    void sortsDescending() {
        List<LeaderboardEntry> entries = List.of(entry(10), entry(100), entry(50));
        List<LeaderboardEntry> sorted = LeaderboardSorter.sort(entries, Order.DESC);
        assertEquals(100, sorted.get(0).getValue());
        assertEquals(50, sorted.get(1).getValue());
        assertEquals(10, sorted.get(2).getValue());
    }

    @Test
    void sortsAscending() {
        List<LeaderboardEntry> entries = List.of(entry(10), entry(100), entry(50));
        List<LeaderboardEntry> sorted = LeaderboardSorter.sort(entries, Order.ASC);
        assertEquals(10, sorted.get(0).getValue());
        assertEquals(50, sorted.get(1).getValue());
        assertEquals(100, sorted.get(2).getValue());
    }

    @Test
    void competitionRankingHandlesTies() {
        // 100, 100, 90 -> ranks 1, 1, 3
        List<LeaderboardEntry> sorted = List.of(entry(100), entry(100), entry(90));
        assertEquals(1, LeaderboardSorter.rankAt(sorted, 0, RankingMode.COMPETITION));
        assertEquals(1, LeaderboardSorter.rankAt(sorted, 1, RankingMode.COMPETITION));
        assertEquals(3, LeaderboardSorter.rankAt(sorted, 2, RankingMode.COMPETITION));
    }

    @Test
    void denseRankingHandlesTies() {
        // 100, 100, 90 -> ranks 1, 1, 2
        List<LeaderboardEntry> sorted = List.of(entry(100), entry(100), entry(90));
        assertEquals(1, LeaderboardSorter.rankAt(sorted, 0, RankingMode.DENSE));
        assertEquals(1, LeaderboardSorter.rankAt(sorted, 1, RankingMode.DENSE));
        assertEquals(2, LeaderboardSorter.rankAt(sorted, 2, RankingMode.DENSE));
    }

    @Test
    void ordinalRankingIgnoresTies() {
        List<LeaderboardEntry> sorted = List.of(entry(100), entry(100), entry(90));
        assertEquals(1, LeaderboardSorter.rankAt(sorted, 0, RankingMode.ORDINAL));
        assertEquals(2, LeaderboardSorter.rankAt(sorted, 1, RankingMode.ORDINAL));
        assertEquals(3, LeaderboardSorter.rankAt(sorted, 2, RankingMode.ORDINAL));
    }

    @Test
    void handlesEmptyList() {
        assertEquals(-1, LeaderboardSorter.rankAt(List.of(), 0, RankingMode.COMPETITION));
    }

    @Test
    void handlesSingleEntry() {
        List<LeaderboardEntry> sorted = List.of(entry(42));
        assertEquals(1, LeaderboardSorter.rankAt(sorted, 0, RankingMode.COMPETITION));
    }

    @Test
    void handlesOutOfBoundsIndex() {
        List<LeaderboardEntry> sorted = List.of(entry(42));
        assertEquals(-1, LeaderboardSorter.rankAt(sorted, 5, RankingMode.COMPETITION));
        assertEquals(-1, LeaderboardSorter.rankAt(sorted, -1, RankingMode.COMPETITION));
    }
}
