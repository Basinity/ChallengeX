package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityMountEvent;

/**
 * {@code trigger.mounted}: a player got on a horse, boat, minecart, or anything
 * else rideable. The {@code vehicle} parameter matches what was mounted.
 */
public final class MountedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMounted(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        context().emit(GameEvent.of("trigger.mounted", player.getName(),
                Map.of("vehicle", ParamValue.of(GameIds.of(event.getMount())))));
    }
}
