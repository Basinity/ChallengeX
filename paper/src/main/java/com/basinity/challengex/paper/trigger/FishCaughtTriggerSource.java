package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerFishEvent;

/** {@code trigger.fish_caught}: a player reeled in a catch. */
public final class FishCaughtTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFishCaught(PlayerFishEvent event) {
        // Casting, reeling in nothing, and hooking an entity are all this event
        // too; only an actual catch counts.
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        context().emit(GameEvent.of("trigger.fish_caught", event.getPlayer().getName()));
    }
}
