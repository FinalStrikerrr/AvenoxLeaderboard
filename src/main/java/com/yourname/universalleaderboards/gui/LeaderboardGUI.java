package com.yourname.universalleaderboards.gui;

import com.yourname.universalleaderboards.config.GuiConfigManager;
import com.yourname.universalleaderboards.leaderboard.*;
import com.yourname.universalleaderboards.leaderboard.LeaderboardType.RankingMode;
import com.yourname.universalleaderboards.player.PlayerResolver;
import com.yourname.universalleaderboards.stats.PlayerStats;
import com.yourname.universalleaderboards.util.ColorUtil;
import com.yourname.universalleaderboards.util.ItemBuilder;
import com.yourname.universalleaderboards.util.NumberFormatter;
import com.yourname.universalleaderboards.util.TextUtil;
import com.yourname.universalleaderboards.util.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Pure rendering logic - takes already-cached data (Leaderboard.getSnapshot(), which is
 * never recalculated here) and produces an Inventory + a GUIPage describing which slot
 * maps to which click action. Never touches the database or PlaceholderAPI directly.
 *
 * The main menu IS the leaderboard selector - there is no intermediate category screen.
 * Empty slots are left empty unless decoration.enabled is turned on in gui.yml.
 */
public class LeaderboardGUI {

    private final GuiConfigManager guiConfig;
    private final PlayerResolver playerResolver;
    private final RankingMode rankingMode;

    public LeaderboardGUI(GuiConfigManager guiConfig, PlayerResolver playerResolver, RankingMode rankingMode) {
        this.guiConfig = guiConfig;
        this.playerResolver = playerResolver;
        this.rankingMode = rankingMode;
    }

    public GUIHolder buildMainMenu() {
        GUIHolder holder = new GUIHolder(GUIHolder.ViewType.MAIN_MENU, null, 0);
        Inventory inventory = Bukkit.createInventory(holder, guiConfig.getMainSize(),
                ColorUtil.colorize(guiConfig.getMainTitle()));
        holder.setInventory(inventory);
        GUIPage page = new GUIPage();

        applyDecoration(inventory);

        for (Map.Entry<String, GuiConfigManager.ButtonConfig> entry : guiConfig.getMainButtons().entrySet()) {
            String key = entry.getKey();
            GuiConfigManager.ButtonConfig button = entry.getValue();
            ItemStack item = buildButtonItem(button);
            inventory.setItem(button.slot, item);

            GUIItem.ClickAction action;
            String actionData;
            switch (key) {
                case "close" -> { action = GUIItem.ClickAction.CLOSE; actionData = null; }
                default -> { action = GUIItem.ClickAction.OPEN_BOARD; actionData = key; }
            }
            page.put(new GUIItem(button.slot, item, action, actionData));
        }

        holder.setRenderedPage(page);
        return holder;
    }

    public GUIHolder buildBoardView(Player viewer, Leaderboard board, int page0) {
        return buildBoardView(viewer, board, page0, defaultSortMode(board));
    }

    public GUIHolder buildBoardView(Player viewer, Leaderboard board, int page0, LeaderboardSorter.SortMode sortMode) {
        LeaderboardSorter.SortMode effectiveMode = sortMode == null ? defaultSortMode(board) : sortMode;
        GUIHolder holder = new GUIHolder(GUIHolder.ViewType.BOARD, board.getId(), page0, effectiveMode);
        Inventory inventory = Bukkit.createInventory(holder, 54, ColorUtil.colorize(board.getDisplayName()));
        holder.setInventory(inventory);
        GUIPage guiPage = new GUIPage();

        applyDecoration(inventory);

        List<Integer> entrySlots = guiConfig.getEntrySlots();
        List<LeaderboardEntry> snapshot = LeaderboardSorter.sortForGui(board.getSnapshot(), effectiveMode);
        int perPage = Math.max(1, Math.min(entrySlots.size(), guiConfig.getEntriesPerPage()));
        int startIndex = Math.max(0, page0) * perPage;

        for (int i = 0; i < perPage; i++) {
            int entryIndex = startIndex + i;
            if (i >= entrySlots.size() || entryIndex >= snapshot.size()) break;

            int slot = entrySlots.get(i);
            LeaderboardEntry entry = snapshot.get(entryIndex);
            int rank = LeaderboardSorter.rankAt(snapshot, entryIndex, rankingMode);

            ItemStack item = buildEntryItem(board, entry, rank);
            inventory.setItem(slot, item);
            guiPage.put(new GUIItem(slot, item, GUIItem.ClickAction.PLAYER_PROFILE, entry.getUuid().toString()));
        }

        if (snapshot.isEmpty()) {
            int firstSlot = entrySlots.isEmpty() ? 22 : entrySlots.get(entrySlots.size() / 2);
            ItemStack empty = new ItemBuilder(Material.PAPER)
                    .name(guiConfig.getColor("secondary") + "No leaderboard data available yet.")
                    .build();
            inventory.setItem(firstSlot, empty);
        }

        GuiConfigManager.ButtonConfig backBtn = guiConfig.getBoardViewButton("back");
        if (backBtn != null) {
            ItemStack item = buildButtonItem(backBtn);
            inventory.setItem(backBtn.slot, item);
            guiPage.put(new GUIItem(backBtn.slot, item, GUIItem.ClickAction.OPEN_MENU, null));
        }

        boolean hasNext = startIndex + perPage < snapshot.size();
        boolean hasPrev = page0 > 0;

        GuiConfigManager.ButtonConfig nextBtn = guiConfig.getBoardViewButton("next");
        if (nextBtn != null) {
            ItemStack item = buildButtonItem(nextBtn);
            inventory.setItem(nextBtn.slot, item);
            if (hasNext) guiPage.put(new GUIItem(nextBtn.slot, item, GUIItem.ClickAction.NEXT_PAGE, board.getId()));
        }

        GuiConfigManager.ButtonConfig prevBtn = guiConfig.getBoardViewButton("previous");
        if (prevBtn != null) {
            ItemStack item = buildButtonItem(prevBtn);
            inventory.setItem(prevBtn.slot, item);
            if (hasPrev) guiPage.put(new GUIItem(prevBtn.slot, item, GUIItem.ClickAction.PREVIOUS_PAGE, board.getId()));
        }

        GuiConfigManager.ButtonConfig sortBtn = guiConfig.getBoardViewButton("sorting");
        if (sortBtn != null) {
            ItemStack item = buildSortingItem(sortBtn, effectiveMode);
            inventory.setItem(sortBtn.slot, item);
            guiPage.put(new GUIItem(sortBtn.slot, item, GUIItem.ClickAction.SORTING, board.getId()));
        }

        GuiConfigManager.ButtonConfig myStatsBtn = guiConfig.getBoardViewButton("my-stats");
        if (myStatsBtn == null) {
            // Backwards compatibility with older gui.yml files.
            myStatsBtn = guiConfig.getBoardViewButton("my-position");
        }
        if (myStatsBtn != null && viewer != null) {
            ItemStack item = buildButtonItem(myStatsBtn);
            inventory.setItem(myStatsBtn.slot, item);
            guiPage.put(new GUIItem(myStatsBtn.slot, item, GUIItem.ClickAction.OPEN_STATS, null));
        }

        holder.setRenderedPage(guiPage);
        return holder;
    }

    private LeaderboardSorter.SortMode defaultSortMode(Leaderboard board) {
        return board.getOrder() == LeaderboardType.Order.ASC
                ? LeaderboardSorter.SortMode.LOWEST_HIGHEST
                : LeaderboardSorter.SortMode.HIGHEST_LOWEST;
    }

    private ItemStack buildSortingItem(GuiConfigManager.ButtonConfig button, LeaderboardSorter.SortMode mode) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&bCurrent Sorting:");
        for (LeaderboardSorter.SortMode option : LeaderboardSorter.SortMode.values()) {
            String prefix = option == mode ? "&b▸ " : "&7| ";
            String text = option == mode ? "&b" + option.getDisplayName() : "&7" + option.getDisplayName();
            lore.add(prefix + text);
        }
        lore.add("");
        lore.add("&e# Click to Change");
        return new ItemBuilder(materialOrDefault(button.material))
                .name(button.name)
                .lore(lore)
                .customModelData(button.customModelData)
                .build();
    }

    public GUIHolder buildStatsView(Player viewer, OfflinePlayer target, PlayerStats stats) {
        GUIHolder holder = new GUIHolder(GUIHolder.ViewType.STATS, null, 0);
        String targetName = target.getName() == null ? target.getUniqueId().toString() : target.getName();
        Inventory inventory = Bukkit.createInventory(holder, guiConfig.getStatsSize(),
                ColorUtil.colorize(TextUtil.replace(guiConfig.getStatsTitle(), Map.of("%player%", targetName))));
        holder.setInventory(inventory);
        GUIPage page = new GUIPage();

        applyDecoration(inventory);

        String firstJoined = formatFirstJoined(target.getFirstPlayed());
        Map<String, String> values = Map.of(
                "profile", "",
                "kills", NumberFormatter.format(stats.kills(), NumberFormatter.Mode.COMMA, 0),
                "deaths", NumberFormatter.format(stats.deaths(), NumberFormatter.Mode.COMMA, 0),
                "money", stats.isMoneyAvailable() ? NumberFormatter.format(stats.money(), NumberFormatter.Mode.COMMA, 2) : "Unavailable",
                "playtime", TimeFormatter.formatSeconds(stats.playtimeSeconds()),
                "blocks-mined", NumberFormatter.format(stats.blocksMined(), NumberFormatter.Mode.COMMA, 0),
                "blocks-placed", NumberFormatter.format(stats.blocksPlaced(), NumberFormatter.Mode.COMMA, 0),
                "mob-kills", NumberFormatter.format(stats.mobKills(), NumberFormatter.Mode.COMMA, 0),
                "first-joined", firstJoined
        );

        for (Map.Entry<String, GuiConfigManager.StatsItemConfig> entry : guiConfig.getStatsItems().entrySet()) {
            String key = entry.getKey();
            GuiConfigManager.StatsItemConfig itemConfig = entry.getValue();
            ItemBuilder builder = new ItemBuilder(materialOrDefault(itemConfig.material)).name(itemConfig.name);

            if (values.containsKey(key)) {
                String value = values.get(key);
                if (key.equals("money") && !stats.isMoneyAvailable()) value = "Unavailable";

                List<String> configuredLore = itemConfig.lore != null ? itemConfig.lore : List.of();
                if (!configuredLore.isEmpty()) {
                    Map<String, String> replacements = new HashMap<>();
                    replacements.put("%value%", value);
                    replacements.put("%player%", targetName);
                    replacements.put("%label%", key);
                    replacements.put("%first-joined%", firstJoined);
                    builder.lore(TextUtil.replace(configuredLore, replacements));
                } else if (itemConfig.valueFormat != null) {
                    String line = key.equals("money") && !stats.isMoneyAvailable()
                            ? guiConfig.getColor("secondary") + "Unavailable"
                            : itemConfig.valueFormat.replace("%value%", value);
                    builder.lore(List.of(ColorUtil.colorize(line)));
                }
                ItemStack item = builder.build();
                inventory.setItem(itemConfig.slot, item);
            } else {
                // No value-format means this is a plain button (e.g. "close"), not a stat display.
                ItemStack item = builder.build();
                inventory.setItem(itemConfig.slot, item);
                if (key.equals("close")) {
                    page.put(new GUIItem(itemConfig.slot, item, GUIItem.ClickAction.CLOSE, null));
                }
            }
        }

        holder.setRenderedPage(page);
        return holder;
    }

    /** Backwards-compatible overload for integrations compiled against the previous renderer API. */
    public GUIHolder buildStatsView(Player viewer, PlayerStats stats) {
        return buildStatsView(viewer, viewer, stats);
    }

    private String formatFirstJoined(long firstPlayed) {
        if (firstPlayed <= 0L) return "Unknown";
        try {
            return java.time.format.DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss")
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(java.time.Instant.ofEpochMilli(firstPlayed));
        } catch (Exception ignored) {
            return "Unknown";
        }
    }

    private ItemStack buildEntryItem(Leaderboard board, LeaderboardEntry entry, int rank) {
        OfflinePlayer player = playerResolver.resolve(entry.getUuid());
        Material material = guiConfig.usePlayerHeads() ? Material.PLAYER_HEAD : Material.PAPER;
        String topIconName = guiConfig.getTopIcon(rank);
        if (!guiConfig.usePlayerHeads() && topIconName != null) material = materialOrDefault(topIconName);

        String label = switch (board.getId().toLowerCase(Locale.ROOT)) {
            case "money" -> "Balance";
            case "kills" -> "Kills";
            case "deaths" -> "Deaths";
            case "blocks-mined" -> "Blocks Mined";
            case "blocks-placed" -> "Blocks Placed";
            case "playtime" -> "Playtime";
            case "mob-kills" -> "Mobs Killed";
            default -> board.getDisplayName();
        };

        String boardColor = boardColor(board);
        Map<String, String> replacements = new HashMap<>();
        replacements.put("%rank%", String.valueOf(rank));
        replacements.put("%player%", entry.getDisplayName());
        replacements.put("%value%", board.formatValue(entry.getValue()));
        replacements.put("%board%", board.getDisplayName());
        replacements.put("%label%", label);
        replacements.put("%color%", boardColor);

        ItemBuilder builder = new ItemBuilder(material)
                .name(boardColor + "#" + rank + " " + entry.getDisplayName())
                .lore(TextUtil.replace(guiConfig.getEntryLore(), replacements))
                .glow(rank == 1);

        if (guiConfig.usePlayerHeads()) builder.skullOwner(player);
        return builder.build();
    }

    private String boardColor(Leaderboard board) {
        return switch (board.getId().toLowerCase(Locale.ROOT)) {
            case "money" -> "&#55d6be";
            case "kills" -> "&#ff6b6b";
            case "deaths" -> "&#c084fc";
            case "blocks-mined" -> "&#60a5fa";
            case "blocks-placed" -> "&#f59e0b";
            case "playtime" -> "&#38bdf8";
            case "mob-kills" -> "&#a3e635";
            default -> guiConfig.getColor("primary");
        };
    }

    private ItemStack buildButtonItem(GuiConfigManager.ButtonConfig button) {
        return new ItemBuilder(materialOrDefault(button.material))
                .name(button.name)
                .lore(button.lore)
                .customModelData(button.customModelData)
                .build();
    }

    private void applyDecoration(Inventory inventory) {
        if (!guiConfig.isDecorationEnabled()) return;
        ItemStack filler = new ItemBuilder(materialOrDefault(guiConfig.getDecorationMaterial())).name(" ").build();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) inventory.setItem(i, filler);
        }
    }

    private Material materialOrDefault(String name) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return Material.STONE;
        }
    }
}
