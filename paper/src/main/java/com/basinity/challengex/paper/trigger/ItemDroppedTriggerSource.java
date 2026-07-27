package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * {@code trigger.item_dropped}: a player threw an item on the ground. The
 * {@code item} parameter matches what was dropped.
 */
public final class ItemDroppedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDropped(PlayerDropItemEvent event) {
        String itemId = event.getItemDrop().getItemStack().getType().getKey().toString();
        context().emit(GameEvent.of("trigger.item_dropped", event.getPlayer().getName(),
                Map.of("item", ParamValue.of(itemId))));
    }
}
