package com.yourname.universalleaderboards.hook;

import com.yourname.universalleaderboards.placeholder.PlaceholderResolver;
import com.yourname.universalleaderboards.scheduler.SchedulerAdapter;
import com.yourname.universalleaderboards.util.NumberFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Resolves economy balances from Vault when available, with PlaceholderAPI fallback. */
public class PlaceholderMoneyProvider implements MoneyProvider {
    private static final String ECONOMY_CLASS = "net.milkbowl.vault.economy.Economy";
    private static final String VAULT_BALANCE_PLACEHOLDER = "%vault_eco_balance%";

    private final PlaceholderResolver resolver;
    private final Executor asyncExecutor;
    private final SchedulerAdapter scheduler;

    public PlaceholderMoneyProvider(PlaceholderResolver resolver, Executor asyncExecutor, SchedulerAdapter scheduler) {
        this.resolver = resolver;
        this.asyncExecutor = asyncExecutor;
        this.scheduler = scheduler;
    }

    private Object getEconomyProvider() {
        try {
            Class<?> economyClass = Class.forName(ECONOMY_CLASS);
            var registration = Bukkit.getServicesManager().getRegistration(economyClass);
            return registration == null ? null : registration.getProvider();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return getEconomyProvider() != null ||
                (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null &&
                 Bukkit.getPluginManager().getPlugin("Vault") != null);
    }

    @Override
    public CompletableFuture<Double> getBalance(UUID uuid) {
        if (!isAvailable()) return CompletableFuture.completedFuture(null);
        CompletableFuture<Double> result = new CompletableFuture<>();
        scheduler.runGlobal(() -> {
            try {
                result.complete(getBalanceSync(uuid));
            } catch (Throwable ignored) {
                result.complete(null);
            }
        });
        return result;
    }

    @Override
    public Double getBalanceSync(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        Object economy = getEconomyProvider();
        if (economy != null) {
            try {
                Method method = economy.getClass().getMethod("getBalance", OfflinePlayer.class);
                Object value = method.invoke(economy, player);
                if (value instanceof Number n) return n.doubleValue();
            } catch (Throwable ignored) {
                // Fall back to PlaceholderAPI below.
            }
        }
        if (resolver != null && Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return NumberFormatter.parse(resolver.resolve(player, VAULT_BALANCE_PLACEHOLDER));
        }
        return null;
    }
}
