package com.yourname.universalleaderboards.stats;

/**
 * @param money null means unavailable (Vault/PlaceholderAPI not present), not zero balance.
 */
public record PlayerStats(
        long kills,
        long deaths,
        Double money,
        long blocksMined,
        long blocksPlaced,
        long playtimeSeconds,
        long mobKills
) {
    public boolean isMoneyAvailable() {
        return money != null;
    }
}
