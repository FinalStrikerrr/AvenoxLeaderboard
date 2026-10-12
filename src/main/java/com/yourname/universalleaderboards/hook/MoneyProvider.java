package com.yourname.universalleaderboards.hook;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Abstracts "what is this player's economy balance" away from how it's actually resolved.
 * The only implementation today (PlaceholderMoneyProvider) goes through PlaceholderAPI's own
 * Vault expansion rather than a direct VaultAPI dependency - see that class for why. The
 * interface exists so a future direct-Vault or other-economy-plugin implementation can be
 * swapped in without touching StatsManager, PlaceholderManager, or the GUI.
 */
public interface MoneyProvider {

    /** Whether a money source is currently hooked at all (e.g. Vault + an economy plugin present). */
    boolean isAvailable();

    /** Resolves a balance, or completes with null if it couldn't be determined for this player. */
    CompletableFuture<Double> getBalance(UUID uuid);

    /** Reads the balance on the server/global thread. */
    default Double getBalanceSync(UUID uuid) {
        return getBalance(uuid).join();
    }
}
