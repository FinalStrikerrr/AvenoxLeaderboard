package com.yourname.universalleaderboards.leaderboard;

public final class LeaderboardType {

    private LeaderboardType() {}

    public enum ValueType { NUMBER, INTEGER, DECIMAL, TIME, BOOLEAN }

    public enum Order { ASC, DESC }

    public enum TimedPeriod { ALL_TIME, HOURLY, DAILY, WEEKLY, MONTHLY, CUSTOM }

    public enum Scope { GLOBAL, WORLD, SERVER }

    public enum RankingMode { COMPETITION, DENSE, ORDINAL }

    /**
     * Where a board's value comes from. PLACEHOLDER is the original generic engine (any
     * PlaceholderAPI placeholder) and is the default when a board's config omits `source:`
     * entirely, so every pre-existing custom leaderboard keeps working unchanged. The other
     * three back the seven built-in boards:
     *   STATISTIC - a plain Bukkit Statistic (kills, deaths, playtime)
     *   VAULT     - an economy balance, resolved via PlaceholderAPI's own Vault expansion
     *               rather than a direct VaultAPI dependency (see MoneyProvider)
     *   COUNTER   - a value maintained by this plugin's own event listeners with atomic
     *               DB increments (blocks mined/placed, mob kills), not resolved per refresh
     */
    public enum SourceType { PLACEHOLDER, STATISTIC, VAULT, COUNTER }
}
