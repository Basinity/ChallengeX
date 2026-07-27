package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * {@code trigger.food_eaten}: a player finished eating something. The
 * {@code item} parameter matches what was eaten.
 */
public final class FoodEatenTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFoodEaten(PlayerItemConsumeEvent event) {
        context().emit(GameEvent.of("trigger.food_eaten", event.getPlayer().getName(),
                Map.of("item", ParamValue.of(event.getItem().getType().getKey().toString()))));
    }
}
