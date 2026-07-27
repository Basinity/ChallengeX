package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * {@code trigger.xp_gained}: a player gained experience points, from a furnace
 * pickup, mob kill, ore, breeding, and the rest. It rides the point award, so a
 * bare level-set command that hands out no points does not trigger it.
 */
public final class XpGainedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onXpGained(PlayerExpChangeEvent event) {
        if (event.getAmount() <= 0) {
            return;
        }
        context().emit(GameEvent.of("trigger.xp_gained", event.getPlayer().getName()));
    }
}
