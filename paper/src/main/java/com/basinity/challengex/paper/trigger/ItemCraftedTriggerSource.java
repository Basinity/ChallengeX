package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * {@code trigger.item_crafted}: a player took a crafted result out of a crafting
 * grid. The {@code item} parameter matches the crafted item.
 */
public final class ItemCraftedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemCrafted(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getRecipe().getResult();
        if (result.isEmpty()) {
            return;
        }
        context().emit(GameEvent.of("trigger.item_crafted", player.getName(),
                Map.of("item", ParamValue.of(result.getType().getKey().toString()))));
    }
}
