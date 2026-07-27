package com.basinity.challengex.paper.modifier;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * {@code modifier.keep_inventory}: the player keeps their inventory and
 * experience on death.
 *
 * <p>Fabric needs two Mixins for this, one to stop the drop and one to carry
 * the items across the respawn. Bukkit puts both on the death event, so the
 * whole modifier is two calls.
 */
public final class KeepInventoryModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        if (!isActive(event.getEntity(), "modifier.keep_inventory")) {
            return;
        }
        event.setKeepInventory(true);
        event.setKeepLevel(true);
        // Kept, so there is nothing left to drop or to strip.
        event.getDrops().clear();
        event.setDroppedExp(0);
    }
}
