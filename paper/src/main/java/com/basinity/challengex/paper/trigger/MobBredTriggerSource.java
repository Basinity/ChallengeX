package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityBreedEvent;

/**
 * {@code trigger.mob_bred}: a player bred two animals. The {@code mob} parameter
 * matches the offspring's type.
 */
public final class MobBredTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobBred(EntityBreedEvent event) {
        if (!(event.getBreeder() instanceof Player breeder)) {
            return;
        }
        context().emit(GameEvent.of("trigger.mob_bred", breeder.getName(),
                Map.of("mob", ParamValue.of(GameIds.of(event.getEntity())))));
    }
}
