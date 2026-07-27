package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerItemBreakEvent;

/**
 * {@code trigger.tool_broke}: a player wore an item out until it broke. The
 * {@code item} parameter matches what broke.
 */
public final class ToolBrokeTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onToolBroke(PlayerItemBreakEvent event) {
        String itemId = event.getBrokenItem().getType().getKey().toString();
        context().emit(GameEvent.of("trigger.tool_broke", event.getPlayer().getName(),
                Map.of("item", ParamValue.of(itemId))));
    }
}
