package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerChangedWorldEvent;

/**
 * {@code trigger.dimension_changed}: a player arrived in a dimension. The
 * {@code dimension} parameter matches the destination's id
 * ({@code minecraft:the_nether}), not the one departed.
 */
public final class DimensionChangedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDimensionChanged(PlayerChangedWorldEvent event) {
        // The world's key, not its folder name: a preset names the nether
        // minecraft:the_nether whatever the server called the folder.
        String dimensionId = event.getPlayer().getWorld().getKey().toString();
        context().emit(GameEvent.of("trigger.dimension_changed", event.getPlayer().getName(),
                Map.of("dimension", ParamValue.of(dimensionId))));
    }
}
