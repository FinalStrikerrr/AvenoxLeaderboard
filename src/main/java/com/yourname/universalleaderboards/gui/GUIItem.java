package com.yourname.universalleaderboards.gui;

import org.bukkit.inventory.ItemStack;

public class GUIItem {

    public enum ClickAction {
        OPEN_BOARD, OPEN_MENU, NEXT_PAGE, PREVIOUS_PAGE,
        SORTING,
        CLOSE, PLAYER_PROFILE, OPEN_STATS, COMMAND, MESSAGE, NONE
    }

    private final int slot;
    private final ItemStack itemStack;
    private final ClickAction action;
    private final String actionData;

    public GUIItem(int slot, ItemStack itemStack, ClickAction action, String actionData) {
        this.slot = slot;
        this.itemStack = itemStack;
        this.action = action;
        this.actionData = actionData;
    }

    public int getSlot() { return slot; }
    public ItemStack getItemStack() { return itemStack; }
    public ClickAction getAction() { return action; }
    public String getActionData() { return actionData; }
}
