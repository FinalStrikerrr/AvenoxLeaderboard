package com.yourname.universalleaderboards.gui;

import java.util.HashMap;
import java.util.Map;

public class GUIPage {

    private final Map<Integer, GUIItem> items = new HashMap<>();

    public void put(GUIItem item) {
        items.put(item.getSlot(), item);
    }

    public GUIItem get(int slot) {
        return items.get(slot);
    }

    public Map<Integer, GUIItem> getItems() {
        return items;
    }
}
