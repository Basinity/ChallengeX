package com.basinity.challengex.paper.modifier;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/**
 * {@code modifier.disable_item_drop}: the player cannot throw items on the
 * ground.
 *
 * <p>Both ways of dropping are covered, since Fabric needed two Mixins for the
 * same reason: the drop key, and dragging an item out of an open inventory
 * screen.
 */
public final class DisableItemDropModifierSource extends EventModifierSource {

    private static final String MODIFIER_ID = "modifier.disable_item_drop";

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (isActive(event.getPlayer(), MODIFIER_ID)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDropFromScreen(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        InventoryAction action = event.getAction();
        if (action != InventoryAction.DROP_ONE_SLOT && action != InventoryAction.DROP_ALL_SLOT
                && action != InventoryAction.DROP_ONE_CURSOR && action != InventoryAction.DROP_ALL_CURSOR) {
            return;
        }
        if (isActive(player, MODIFIER_ID)) {
            event.setCancelled(true);
        }
    }
}
