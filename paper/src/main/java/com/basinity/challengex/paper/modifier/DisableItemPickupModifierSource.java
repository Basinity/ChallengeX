package com.basinity.challengex.paper.modifier;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;

/** {@code modifier.disable_item_pickup}: the player cannot pick items up. */
public final class DisableItemPickupModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player
                && isActive(player, "modifier.disable_item_pickup")) {
            event.setCancelled(true);
        }
    }
}
