package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * {@code trigger.kill_player}: a player killed another player. The {@code name}
 * parameter matches the victim's name; omitting it fires on any player kill.
 * This is the killer side of a player death, distinct from
 * {@code trigger.player_died} (which fires on the victim) and
 * {@code trigger.mob_killed} (which excludes player victims entirely).
 */
public final class KillPlayerTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKilled(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer == victim) {
            return;
        }
        context().emit(GameEvent.of("trigger.kill_player", killer.getName(),
                Map.of("name", ParamValue.of(victim.getName()))));
    }
}
