package com.yourname.universalleaderboards.listener;

import com.yourname.universalleaderboards.stats.PersistentCounterService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * Event-driven rather than Statistic.MINE_BLOCK-based: MINE_BLOCK is tracked per-Material
 * (300+ values), and summing it for every online player on every refresh cycle would mean
 * players x materials Bukkit calls per cycle - a genuinely bad performance trade for a
 * simple running total. One increment per broken block is O(1) and matches the same pattern
 * already used for blocks-placed and mob-kills.
 */
public class BlockBreakListener implements Listener {

    public static final String BOARD_ID = "blocks-mined";

    private final PersistentCounterService counters;

    public BlockBreakListener(PersistentCounterService counters) {
        this.counters = counters;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        counters.increment(BOARD_ID, event.getPlayer(), 1);
    }
}
