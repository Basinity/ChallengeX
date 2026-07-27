package com.basinity.challengex.paper.modifier;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * The refusals half of {@code modifier.item_lock}: an item somebody else in the
 * group holds never reaches this player's hands in the first place, so
 * {@link ItemLockEnforcer}'s ejection stays the backstop rather than the normal
 * experience.
 *
 * <p>Two paths are covered, which between them are how a locked item is nearly
 * always acquired: picking one up off the ground, and taking one out of a
 * container, a crafting or furnace output, or a trade. What is left over falls
 * through to the ejection, which is the same division the Fabric adapter makes.
 */
public final class ItemLockModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player
                && ItemLocks.blocked(player.getUniqueId(), event.getItem().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTake(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack taken = event.getCurrentItem();
        if (taken != null && ItemLocks.blocked(player.getUniqueId(), taken)) {
            event.setCancelled(true);
        }
    }
}
