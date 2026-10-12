package com.yourname.universalleaderboards.listener;

import com.yourname.universalleaderboards.stats.PersistentCounterService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockPlaceListener implements Listener {

    public static final String BOARD_ID = "blocks-placed";

    private final PersistentCounterService counters;

    public BlockPlaceListener(PersistentCounterService counters) {
        this.counters = counters;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        // MONITOR + ignoreCancelled: only counts a block that actually ended up placed,
        // after every other plugin (protection/anti-grief/etc.) has had a chance to cancel it.
        counters.increment(BOARD_ID, event.getPlayer(), 1);
    }
}
