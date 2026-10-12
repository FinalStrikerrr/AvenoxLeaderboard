package com.yourname.universalleaderboards;

import com.yourname.universalleaderboards.api.UniversalLeaderboardAPI;
import com.yourname.universalleaderboards.command.CommandCompleter;
import com.yourname.universalleaderboards.command.LeaderboardAdminCommand;
import com.yourname.universalleaderboards.command.LeaderboardCommand;
import com.yourname.universalleaderboards.command.StatsCommand;
import com.yourname.universalleaderboards.command.StatsTabCompleter;
import com.yourname.universalleaderboards.compat.CompatibilityManager;
import com.yourname.universalleaderboards.compat.VersionAdapter;
import com.yourname.universalleaderboards.config.ConfigManager;
import com.yourname.universalleaderboards.config.GuiConfigManager;
import com.yourname.universalleaderboards.config.LeaderboardConfigManager;
import com.yourname.universalleaderboards.config.MessageManager;
import com.yourname.universalleaderboards.cache.LeaderboardCache;
import com.yourname.universalleaderboards.gui.GUIManager;
import com.yourname.universalleaderboards.gui.LeaderboardGUI;
import com.yourname.universalleaderboards.hook.LuckPermsHook;
import com.yourname.universalleaderboards.hook.MoneyProvider;
import com.yourname.universalleaderboards.hook.PlaceholderMoneyProvider;
import com.yourname.universalleaderboards.leaderboard.LeaderboardManager;
import com.yourname.universalleaderboards.leaderboard.LeaderboardUpdater;
import com.yourname.universalleaderboards.listener.BlockBreakListener;
import com.yourname.universalleaderboards.listener.BlockPlaceListener;
import com.yourname.universalleaderboards.listener.MobKillListener;
import com.yourname.universalleaderboards.placeholder.PlaceholderManager;
import com.yourname.universalleaderboards.placeholder.PlaceholderResolver;
import com.yourname.universalleaderboards.placeholder.UniversalLeaderboardsExpansion;
import com.yourname.universalleaderboards.player.PlayerResolver;
import com.yourname.universalleaderboards.scheduler.SchedulerAdapter;
import com.yourname.universalleaderboards.scheduler.SchedulerFactory;
import com.yourname.universalleaderboards.stats.PersistentCounterService;
import com.yourname.universalleaderboards.stats.StatsManager;
import com.yourname.universalleaderboards.listener.PlayerJoinListener;
import com.yourname.universalleaderboards.storage.StorageManager;
import com.yourname.universalleaderboards.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.Executor;

public class UniversalLeaderboardsPlugin extends JavaPlugin {

    /** How often buffered block/mob counter deltas get flushed to storage as atomic increments. */
    private static final long COUNTER_FLUSH_INTERVAL_TICKS = 100L; // 5 seconds

    private ConfigManager configManager;
    private MessageManager messageManager;
    private GuiConfigManager guiConfigManager;
    private LeaderboardConfigManager leaderboardConfigManager;

    private SchedulerAdapter scheduler;
    private StorageManager storageManager;
    private StorageProvider storageProvider;
    private LeaderboardCache leaderboardCache;
    private PlaceholderResolver placeholderResolver;
    private PlaceholderManager placeholderManager;
    private LeaderboardManager leaderboardManager;
    private LeaderboardUpdater leaderboardUpdater;
    private PlayerResolver playerResolver;
    private MoneyProvider moneyProvider;
    private StatsManager statsManager;
    private PersistentCounterService counterService;
    private SchedulerAdapter.CancellableTask counterFlushTask;

    private GUIManager guiManager;
    private UniversalLeaderboardsExpansion expansion;

    private LuckPermsHook luckPermsHook;

    private boolean placeholderApiPresent;

    @Override
    public void onEnable() {
        long start = System.currentTimeMillis();

        // 1. Detect server implementation.
        CompatibilityManager.detect();

        // 2. Load configuration.
        configManager = new ConfigManager(this);
        configManager.load();
        messageManager = new MessageManager(this);
        messageManager.load();
        guiConfigManager = new GuiConfigManager(this);
        guiConfigManager.load();
        leaderboardConfigManager = new LeaderboardConfigManager(this, getLogger());
        leaderboardConfigManager.load();

        // 3. Initialize scheduler adapter (Folia-aware).
        scheduler = SchedulerFactory.create(this);

        // 4. Check PlaceholderAPI - hard requirement for placeholder-driven boards,
        //    but the plugin must not crash if it's missing (section 39/42).
        placeholderApiPresent = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (!placeholderApiPresent) {
            getLogger().warning("PlaceholderAPI was not found. Boards using PlaceholderAPI placeholders will not update.");
            getLogger().warning("Statistic-, Vault-, and counter-backed boards (the seven default boards) will continue to function normally.");
        }

        // 5. Initialize storage (async).
        storageManager = new StorageManager(this);
        Executor asyncExecutor = command -> scheduler.runAsync(command);
        try {
            storageProvider = storageManager.initialize(configManager.getStorageConfig(), asyncExecutor, getLogger());
        } catch (Exception e) {
            getLogger().severe("Fatal error initializing storage - disabling plugin: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 6. Initialize cache.
        leaderboardCache = new LeaderboardCache(configManager.getPlaceholderCacheMs());

        // 7. Hook PlaceholderAPI + Money (Vault, via PlaceholderAPI's own expansion - see
        //    PlaceholderMoneyProvider for why this plugin never compiles against VaultAPI directly).
        placeholderResolver = new PlaceholderResolver(placeholderApiPresent, getLogger(), configManager.isPlaceholderLogging());
        moneyProvider = new PlaceholderMoneyProvider(placeholderResolver, asyncExecutor, scheduler);
        placeholderManager = new PlaceholderManager(placeholderResolver, leaderboardCache, moneyProvider,
                getLogger(), configManager.getMaxPlayerUpdatesPerCycle(), scheduler);

        // 8. Hook optional plugins.
        luckPermsHook = new LuckPermsHook();
        luckPermsHook.setup(this);

        // 9. Load leaderboards.
        leaderboardUpdater = new LeaderboardUpdater(placeholderManager, storageProvider, getLogger(), configManager.isDebugLogging());
        leaderboardManager = new LeaderboardManager(leaderboardUpdater, scheduler, storageProvider, getLogger());
        leaderboardConfigManager.populate(leaderboardManager);

        playerResolver = new PlayerResolver();
        statsManager = new StatsManager(storageProvider, moneyProvider);

        // 10. Initialize the counter service BEFORE constructing GUIManager.
        // GUIManager uses it to flush blocks/mob counters immediately when a counter
        // leaderboard is opened. Constructing GUIManager before this service existed
        // passed null and made the on-open flush silently unavailable.
        counterService = new PersistentCounterService(storageProvider, getLogger());

        // 11. Register commands.
        LeaderboardGUI guiRenderer = new LeaderboardGUI(guiConfigManager, playerResolver, configManager.getRankingMode());
        guiManager = new GUIManager(guiRenderer, leaderboardManager, guiConfigManager, messageManager, storageProvider, statsManager, scheduler, getLogger(), counterService);
        getServer().getPluginManager().registerEvents(guiManager, this);

        LeaderboardAdminCommand adminCommand = new LeaderboardAdminCommand(
                leaderboardManager, leaderboardConfigManager, messageManager, storageProvider, placeholderResolver, this::reload);
        LeaderboardCommand rootCommand = new LeaderboardCommand(leaderboardManager, guiManager, messageManager, storageProvider, adminCommand);

        var leaderboardCommand = getCommand("leaderboard");
        if (leaderboardCommand != null) {
            leaderboardCommand.setExecutor(rootCommand);
            leaderboardCommand.setTabCompleter(new CommandCompleter(leaderboardManager));
        }

        var statsCommand = getCommand("stats");
        if (statsCommand != null) {
            statsCommand.setExecutor(new StatsCommand(guiManager, messageManager));
            statsCommand.setTabCompleter(new StatsTabCompleter());
        }

        // 12. Register listeners - GUIManager plus the three event-driven counter boards.
        getServer().getPluginManager().registerEvents(new BlockPlaceListener(counterService), this);
        getServer().getPluginManager().registerEvents(new BlockBreakListener(counterService), this);
        getServer().getPluginManager().registerEvents(new MobKillListener(counterService), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(leaderboardManager), this);

        // 13. Register API.
        new UniversalLeaderboardAPI(leaderboardManager, storageProvider, placeholderManager);

        // Register our own PlaceholderAPI expansion (section 9/35 - hologram/external support).
        if (placeholderApiPresent) {
            expansion = new UniversalLeaderboardsExpansion(this, leaderboardManager, storageProvider);
            expansion.register();
        }

        // 14. Start asynchronous update tasks.
        leaderboardManager.startScheduler();
        counterFlushTask = scheduler.runAsyncRepeating(counterService::flush, COUNTER_FLUSH_INTERVAL_TICKS, COUNTER_FLUSH_INTERVAL_TICKS);

        // 15. Print clean startup summary.
        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("AvenoxLeaderboard enabled in " + elapsed + "ms!");
        getLogger().info("Boards: " + leaderboardManager.getBoards().size());
        getLogger().info("Storage: " + storageManager.getTypeInUse());
        getLogger().info("PlaceholderAPI: " + (placeholderApiPresent ? "Enabled" : "Not found"));
        getLogger().info("Vault/Money: " + (moneyProvider.isAvailable() ? "Enabled" : "Unavailable (Vault or PlaceholderAPI missing)"));
        getLogger().info("Folia: " + (CompatibilityManager.isFolia() ? "Detected" : "Not detected (Paper)"));
        getLogger().info("Cache: " + (configManager.isCacheEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("LuckPerms: " + (luckPermsHook.isEnabled() ? "Detected" : "Not detected"));
        getLogger().info("Server version: " + VersionAdapter.serverVersion());
    }

    @Override
    public void onDisable() {
        if (expansion != null) {
            expansion.unregister();
        }
        if (counterFlushTask != null) {
            counterFlushTask.cancel();
        }
        if (counterService != null) {
            // Final flush must complete BEFORE storage closes below, or the last batch of
            // block/mob activity is lost - possibly written against an already-closed pool.
            // A bounded join() is safer than an unbounded one during shutdown: Paper gives
            // plugins a limited window to disable, so block briefly rather than indefinitely.
            try {
                counterService.flush().get(5, java.util.concurrent.TimeUnit.SECONDS);
            } catch (Exception ex) {
                getLogger().warning("[AvenoxLeaderboard] Final counter flush did not complete cleanly: " + ex.getMessage());
            }
        }
        if (leaderboardManager != null) {
            leaderboardManager.stopScheduler();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (storageManager != null) {
            storageManager.close();
        }
        getLogger().info("AvenoxLeaderboard disabled cleanly.");
    }

    /** Full config reload without a server restart (section 41). Cancels/recreates the scheduler task. */
    public void reload() {
        leaderboardManager.stopScheduler();

        configManager.reload();
        messageManager.reload();
        guiConfigManager.reload();
        leaderboardConfigManager.reload();
        leaderboardConfigManager.populate(leaderboardManager);
        leaderboardCache.clear();

        leaderboardManager.startScheduler();
    }

    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public StorageProvider getStorageProvider() { return storageProvider; }
    public SchedulerAdapter getScheduler() { return scheduler; }
}
