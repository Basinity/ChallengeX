package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * {@code trigger.damage_dealt}: a player dealt damage to something. It fires on
 * the damage landing, whether or not the target survives it. A hit that lands
 * but deals no damage still counts; a hit the target blocked with a shield does
 * not, mirroring {@code trigger.damage_taken}, so neither side of a blocked hit
 * fires. The {@code source} parameter matches the damage type dealt ({@code
 * minecraft:player_attack}) and {@code target} matches the entity that was hit;
 * either omitted matches anything.
 */
public final class DamageDealtTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageDealt(EntityDamageEvent event) {
        if (Blocking.wasBlocked(event)
                || !(event.getDamageSource().getCausingEntity() instanceof Player attacker)) {
            return;
        }
        context().emit(GameEvent.of("trigger.damage_dealt", attacker.getName(),
                Map.of("source", ParamValue.of(GameIds.of(event.getDamageSource())),
                        "target", ParamValue.of(GameIds.of(event.getEntity())))));
    }
}
