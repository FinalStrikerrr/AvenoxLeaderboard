package com.yourname.universalleaderboards.listener;

import com.yourname.universalleaderboards.stats.PersistentCounterService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * Counts non-player mob kills only. A dying Player is never counted here (PvP kills already
 * have their own dedicated "kills" board via Statistic.PLAYER_KILLS), and a kill only counts
 * if the killer is a Player (not a dispenser, another mob, fall damage, fire, etc.).
 */
public class MobKillListener implements Listener {

    public static final String BOARD_ID = "mob-kills";

    private final PersistentCounterService counters;

    public MobKillListener(PersistentCounterService counters) {
        this.counters = counters;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Player) return; // PvP is tracked separately by the kills board.

        Player killer = dead.getKiller();
        if (killer == null) return;

        counters.increment(BOARD_ID, killer, 1);
    }
}
