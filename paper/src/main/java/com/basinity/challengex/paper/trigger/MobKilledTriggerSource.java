package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;

/**
 * {@code trigger.mob_killed}: a player killed a mob. The {@code mob} parameter
 * matches the mob's entity type id. Players dying is
 * {@code trigger.player_died} instead, so player kills are not mob kills.
 */
public final class MobKilledTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobKilled(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        context().emit(GameEvent.of("trigger.mob_killed", killer.getName(),
                Map.of("mob", ParamValue.of(GameIds.of(event.getEntity())))));
    }
}
