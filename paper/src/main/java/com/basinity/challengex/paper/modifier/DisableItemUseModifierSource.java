package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.core.model.Modifier;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

/**
 * {@code modifier.disable_item_use}: blocks right-click item use outright for
 * as long as the modifier is active, by failing the interaction rather than
 * detecting and penalizing it after the fact. The optional {@code item}
 * parameter restricts this to one item; omitting it blocks any item, matching
 * how {@code trigger.item_used}'s own {@code item} parameter works.
 */
public final class DisableItemUseModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Modifier modifier = find(event.getPlayer(), "modifier.disable_item_use").orElse(null);
        if (modifier == null) {
            return;
        }
        String restrictTo = ModifierParams.string(modifier, "item");
        if (restrictTo == null) {
            event.setCancelled(true);
            return;
        }
        ItemStack held = event.getItem();
        String itemId = held == null || held.isEmpty() ? null : held.getType().getKey().toString();
        if (restrictTo.equals(itemId)) {
            event.setCancelled(true);
        }
    }
}
