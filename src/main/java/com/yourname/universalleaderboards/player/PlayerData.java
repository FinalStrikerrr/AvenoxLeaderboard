package com.yourname.universalleaderboards.player;

import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private String activeBoard;
    private String activeCategory;
    private int page = 0;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() { return uuid; }
    public String getActiveBoard() { return activeBoard; }
    public void setActiveBoard(String activeBoard) { this.activeBoard = activeBoard; this.page = 0; }
    public String getActiveCategory() { return activeCategory; }
    public void setActiveCategory(String activeCategory) { this.activeCategory = activeCategory; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(0, page); }
}
