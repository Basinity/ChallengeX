package com.basinity.challengex.paper.modifier;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;

/**
 * {@code modifier.no_natural_regen}: the player does not heal on their own.
 *
 * <p>Only the regeneration the body does by itself is stopped. Healing from a
 * potion, a golden apple, or a ChallengeX effect still lands, so the modifier
 * takes away the free recovery rather than making a player unhealable.
 */
public final class NoNaturalRegenModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EntityRegainHealthEvent.RegainReason reason = event.getRegainReason();
        if (reason != EntityRegainHealthEvent.RegainReason.REGEN
                && reason != EntityRegainHealthEvent.RegainReason.SATIATED) {
            return;
        }
        if (isActive(player, "modifier.no_natural_regen")) {
            event.setCancelled(true);
        }
    }
}
