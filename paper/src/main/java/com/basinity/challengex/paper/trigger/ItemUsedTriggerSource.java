package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * {@code trigger.item_used}: a player right-clicked to use a held item. The
 * {@code item} parameter matches the used item; omitting it fires on any use.
 * Using with an empty hand is not a use, so an empty hand never fires it.
 */
public final class ItemUsedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemUsed(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack held = event.getItem();
        if (held == null || held.isEmpty()) {
            return;
        }
        context().emit(GameEvent.of("trigger.item_used", event.getPlayer().getName(),
                Map.of("item", ParamValue.of(held.getType().getKey().toString()))));
    }
}
