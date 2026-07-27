package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * {@code trigger.damage_taken}: a player was hit. A hit that lands but deals
 * no damage still counts; a hit blocked by a shield does not (that's {@code
 * trigger.shield_blocked}'s job). The {@code source} parameter matches the
 * damage type id ({@code minecraft:lava}); omitting it fires on any damage.
 */
public final class DamageTakenTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageTaken(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || Blocking.wasBlocked(event)) {
            return;
        }
        context().emit(GameEvent.of("trigger.damage_taken", player.getName(),
                Map.of("source", ParamValue.of(GameIds.of(event.getDamageSource())))));
    }
}
