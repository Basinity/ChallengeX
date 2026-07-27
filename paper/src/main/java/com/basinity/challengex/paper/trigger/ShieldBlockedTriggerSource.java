package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * {@code trigger.shield_blocked}: a player blocked an attack with a shield.
 * Exactly the hits the two damage triggers skip, so between the three of them a
 * blocked hit is reported once and by this one only.
 */
public final class ShieldBlockedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShieldBlocked(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || !Blocking.wasBlocked(event)) {
            return;
        }
        context().emit(GameEvent.of("trigger.shield_blocked", player.getName()));
    }
}
