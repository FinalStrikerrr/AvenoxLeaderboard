package com.yourname.universalleaderboards.leaderboard;

import java.util.UUID;

public final class LeaderboardEntry {

    private final UUID uuid;
    private String username;
    private String displayName;
    private double value;
    private String rawValue;
    private long updatedAt;

    public LeaderboardEntry(UUID uuid, String username, String displayName, double value, String rawValue, long updatedAt) {
        this.uuid = uuid;
        this.username = username;
        this.displayName = displayName;
        this.value = value;
        this.rawValue = rawValue;
        this.updatedAt = updatedAt;
    }

    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getDisplayName() { return displayName == null ? username : displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getRawValue() { return rawValue; }
    public void setRawValue(String rawValue) { this.rawValue = rawValue; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
