package com.yourname.universalleaderboards.leaderboard;

import com.yourname.universalleaderboards.leaderboard.LeaderboardType.*;
import com.yourname.universalleaderboards.util.NumberFormatter;
import com.yourname.universalleaderboards.util.TimeFormatter;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a single leaderboard's configuration and its current in-memory sorted view.
 * The sorted view is a plain copy-on-write snapshot swapped atomically by LeaderboardUpdater,
 * so GUI reads never block on writers and never see a half-sorted list.
 */
public final class Leaderboard {

    private final String id;
    private String displayName;
    private String placeholder;
    private ValueType valueType = ValueType.NUMBER;
    private SourceType source = SourceType.PLACEHOLDER;
    private String statistic;
    private double statisticDivisor = 1.0;
    private Order order = Order.DESC;
    private TimedPeriod timedPeriod = TimedPeriod.ALL_TIME;
    private int customIntervalSeconds = 0;
    private int updateIntervalSeconds = 30;
    private int maxEntries = 100;
    private String category;
    private boolean enabled = true;
    private NumberFormatter.Mode formatMode = NumberFormatter.Mode.SHORT;
    private String formatPrefix = "";
    private String formatSuffix = "";
    private List<String> extraPlaceholders = Collections.emptyList();
    private List<String> worldFilter = Collections.emptyList();
    private Scope scope = Scope.GLOBAL;
    private String permissionFilter;
    private String customProvider;

    private volatile List<LeaderboardEntry> sortedEntries = Collections.emptyList();
    private final AtomicLong lastRefreshed = new AtomicLong(0);
    private volatile long periodStart = System.currentTimeMillis();

    public Leaderboard(String id) {
        this.id = id;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName == null ? id : displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPlaceholder() { return placeholder; }
    public void setPlaceholder(String placeholder) { this.placeholder = placeholder; }
    public ValueType getValueType() { return valueType; }
    public void setValueType(ValueType valueType) { this.valueType = valueType; }
    public SourceType getSource() { return source; }
    public void setSource(SourceType source) { this.source = source; }
    /** Bukkit Statistic name (e.g. "PLAYER_KILLS"), only meaningful when source == STATISTIC. */
    public String getStatistic() { return statistic; }
    public void setStatistic(String statistic) { this.statistic = statistic; }
    /** Divides the raw Bukkit statistic value before storing/displaying it - e.g. 20 to
     *  convert a tick-based statistic (like PLAY_ONE_MINUTE) into seconds. Defaults to 1
     *  (no conversion). Only meaningful when source == STATISTIC. */
    public double getStatisticDivisor() { return statisticDivisor; }
    public void setStatisticDivisor(double statisticDivisor) { this.statisticDivisor = statisticDivisor <= 0 ? 1.0 : statisticDivisor; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public TimedPeriod getTimedPeriod() { return timedPeriod; }
    public void setTimedPeriod(TimedPeriod timedPeriod) { this.timedPeriod = timedPeriod; }
    public int getCustomIntervalSeconds() { return customIntervalSeconds; }
    public void setCustomIntervalSeconds(int customIntervalSeconds) { this.customIntervalSeconds = customIntervalSeconds; }
    public int getUpdateIntervalSeconds() { return updateIntervalSeconds; }
    public void setUpdateIntervalSeconds(int updateIntervalSeconds) { this.updateIntervalSeconds = updateIntervalSeconds; }
    public int getMaxEntries() { return maxEntries; }
    public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public NumberFormatter.Mode getFormatMode() { return formatMode; }
    public void setFormatMode(NumberFormatter.Mode formatMode) { this.formatMode = formatMode; }
    public String getFormatPrefix() { return formatPrefix; }
    public void setFormatPrefix(String formatPrefix) { this.formatPrefix = formatPrefix; }
    public String getFormatSuffix() { return formatSuffix; }
    public void setFormatSuffix(String formatSuffix) { this.formatSuffix = formatSuffix; }
    public List<String> getExtraPlaceholders() { return extraPlaceholders; }
    public void setExtraPlaceholders(List<String> extraPlaceholders) { this.extraPlaceholders = extraPlaceholders; }
    public List<String> getWorldFilter() { return worldFilter; }
    public void setWorldFilter(List<String> worldFilter) { this.worldFilter = worldFilter; }
    public Scope getScope() { return scope; }
    public void setScope(Scope scope) { this.scope = scope; }
    public String getPermissionFilter() { return permissionFilter; }
    public void setPermissionFilter(String permissionFilter) { this.permissionFilter = permissionFilter; }
    public String getCustomProvider() { return customProvider; }
    public void setCustomProvider(String customProvider) { this.customProvider = customProvider; }

    public long getPeriodStart() { return periodStart; }
    public void setPeriodStart(long periodStart) { this.periodStart = periodStart; }

    /** Atomically replaces the cached sorted snapshot. Called only by LeaderboardUpdater. */
    public void updateSnapshot(List<LeaderboardEntry> newEntries) {
        this.sortedEntries = new CopyOnWriteArrayList<>(newEntries);
        this.lastRefreshed.set(System.currentTimeMillis());
    }

    public List<LeaderboardEntry> getSnapshot() {
        return sortedEntries;
    }

    public long getLastRefreshed() {
        return lastRefreshed.get();
    }

    public boolean isStale() {
        return System.currentTimeMillis() - lastRefreshed.get() > updateIntervalSeconds * 1000L;
    }

    public String formatValue(double value) {
        if (valueType == ValueType.TIME) {
            // Per spec: raw TIME values are expected in seconds (e.g. 604800 -> "7d 0h 0m").
            // If a placeholder reports ticks instead, convert it before storing the entry
            // rather than here, so the stored `value` used for sorting stays consistent.
            return formatPrefix + TimeFormatter.formatSeconds((long) value) + formatSuffix;
        }
        return formatPrefix + NumberFormatter.format(value, formatMode, 2) + formatSuffix;
    }
}
