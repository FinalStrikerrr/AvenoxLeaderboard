package com.yourname.universalleaderboards.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.OfflinePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ItemBuilder {

    private final ItemStack stack;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this(material, 1);
    }

    public ItemBuilder(Material material, int amount) {
        this.stack = new ItemStack(material == null ? Material.STONE : material, Math.max(1, amount));
        this.meta = stack.getItemMeta();
    }

    public ItemBuilder name(String name) {
        if (meta != null && name != null) meta.setDisplayName(ColorUtil.colorize(name));
        return this;
    }

    public ItemBuilder lore(List<String> lore) {
        if (meta != null && lore != null) {
            List<String> colored = new ArrayList<>(lore.size());
            for (String line : lore) colored.add(ColorUtil.colorize(line));
            meta.setLore(colored);
        }
        return this;
    }

    /**
     * Makes the item glow without adding a real (and therefore tooltip-visible/removable)
     * enchantment. Uses the enchantment-glint-override item component added alongside
     * Minecraft's 1.20.5 data component rework, which Paper exposes directly on ItemMeta -
     * this is the correct modern replacement for the old "fake enchant + HIDE_ENCHANTS flag"
     * trick, and avoids depending on any specific Enchantment constant (those were relocated
     * to a keyed registry in the same API generation and are a common source of version drift).
     */
    public ItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        return this;
    }

    public ItemBuilder customModelData(Integer data) {
        if (meta != null && data != null) meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder skullOwner(OfflinePlayer player) {
        if (meta instanceof SkullMeta skullMeta && player != null) {
            skullMeta.setOwningPlayer(player);
        }
        return this;
    }

    public ItemBuilder skullOwner(UUID uuid) {
        return skullOwner(org.bukkit.Bukkit.getOfflinePlayer(uuid));
    }

    public ItemStack build() {
        if (meta != null) stack.setItemMeta(meta);
        return stack;
    }
}
