package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityTameEvent;

/**
 * {@code trigger.mob_tamed}: a player tamed an animal. The {@code mob}
 * parameter matches what was tamed.
 */
public final class MobTamedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobTamed(EntityTameEvent event) {
        if (!(event.getOwner() instanceof Player owner)) {
            return;
        }
        context().emit(GameEvent.of("trigger.mob_tamed", owner.getName(),
                Map.of("mob", ParamValue.of(GameIds.of(event.getEntity())))));
    }
}
