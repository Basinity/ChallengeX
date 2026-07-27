package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.FurnaceExtractEvent;

/**
 * {@code trigger.item_smelted}: a player took a smelted result out of a furnace,
 * smoker, or blast furnace. The {@code item} parameter matches the result.
 */
public final class ItemSmeltedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemSmelted(FurnaceExtractEvent event) {
        context().emit(GameEvent.of("trigger.item_smelted", event.getPlayer().getName(),
                Map.of("item", ParamValue.of(event.getItemType().getKey().toString()))));
    }
}
