package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;

/**
 * {@code trigger.item_picked_up}: a player picked an item up off the ground.
 * The {@code item} parameter matches what was picked up.
 */
public final class ItemPickedUpTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickedUp(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        String itemId = event.getItem().getItemStack().getType().getKey().toString();
        context().emit(GameEvent.of("trigger.item_picked_up", player.getName(),
                Map.of("item", ParamValue.of(itemId))));
    }
}
