package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/** {@code trigger.crit_landed}: a player landed a critical hit. */
public final class CritLandedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCritLanded(EntityDamageByEntityEvent event) {
        if (!event.isCritical() || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        context().emit(GameEvent.of("trigger.crit_landed", attacker.getName()));
    }
}
