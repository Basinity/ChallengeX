package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerBedEnterEvent;

/**
 * {@code trigger.slept}: a player started sleeping. It fires on getting into
 * the bed, not on the night passing, so a refused sleep never reaches it.
 */
public final class SleepTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSlept(PlayerBedEnterEvent event) {
        // Anything but OK means the bed refused them, which is not sleeping.
        if (event.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        context().emit(GameEvent.of("trigger.slept", event.getPlayer().getName()));
    }
}
