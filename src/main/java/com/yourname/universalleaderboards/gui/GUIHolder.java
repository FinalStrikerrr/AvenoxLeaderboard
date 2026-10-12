package com.yourname.universalleaderboards.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import com.yourname.universalleaderboards.leaderboard.LeaderboardSorter;
import org.jetbrains.annotations.NotNull;

public class GUIHolder implements InventoryHolder {

    public enum ViewType { MAIN_MENU, BOARD, STATS }

    private Inventory inventory;
    private final ViewType viewType;
    private final String boardId;
    private final int page;
    private GUIPage renderedPage;
    private LeaderboardSorter.SortMode sortMode;

    public GUIHolder(ViewType viewType, String boardId, int page) {
        this(viewType, boardId, page, LeaderboardSorter.SortMode.HIGHEST_LOWEST);
    }

    public GUIHolder(ViewType viewType, String boardId, int page, LeaderboardSorter.SortMode sortMode) {
        this.viewType = viewType;
        this.boardId = boardId;
        this.page = page;
        this.sortMode = sortMode == null ? LeaderboardSorter.SortMode.HIGHEST_LOWEST : sortMode;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public ViewType getViewType() { return viewType; }
    public String getBoardId() { return boardId; }
    public int getPage() { return page; }
    public GUIPage getRenderedPage() { return renderedPage; }
    public void setRenderedPage(GUIPage renderedPage) { this.renderedPage = renderedPage; }
    public LeaderboardSorter.SortMode getSortMode() { return sortMode; }
}
