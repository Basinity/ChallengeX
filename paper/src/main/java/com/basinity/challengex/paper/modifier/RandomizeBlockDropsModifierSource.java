package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.core.model.Modifier;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;

/**
 * {@code modifier.randomize_block_drops}: what a broken block drops is swapped
 * for a different item, the same substitute every time for a given seed.
 *
 * <p>Where Fabric hooks the loot table with the drops still mutable, Bukkit
 * fires an event carrying the dropped item entities, so each one's stack is
 * rewritten in place and the count is kept.
 */
public final class RandomizeBlockDropsModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockDrops(BlockDropItemEvent event) {
        Player player = event.getPlayer();
        Modifier modifier = find(player, "modifier.randomize_block_drops").orElse(null);
        if (modifier == null) {
            return;
        }
        for (Item dropped : event.getItems()) {
            ItemStack stack = dropped.getItemStack();
            if (stack.isEmpty()) {
                continue;
            }
            Material substitute = RandomizedItemSubstitution.substituteFor(
                    stack.getType(), modifier, player.getName());
            dropped.setItemStack(new ItemStack(substitute, stack.getAmount()));
        }
    }
}
