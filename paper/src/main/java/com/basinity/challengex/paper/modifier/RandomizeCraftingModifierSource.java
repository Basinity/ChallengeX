package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.core.model.Modifier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

/**
 * {@code modifier.randomize_crafting}: every recipe produces something else.
 *
 * <p>By default the result slot itself is rewritten, so the preview tells the
 * truth: you see the rabbit's foot before you commit to crafting it.
 * {@code hide_result} turns that around, leaving the slot showing the recipe's
 * own item and doing the swap as the result is taken, so what comes out is a
 * surprise.
 *
 * <p>The substitution is the same seeded, deterministic mapping the drop
 * randomizers use, so one recipe always produces the same thing for a given
 * seed and the run stays learnable. The count the recipe asked for is kept: a
 * recipe yielding four planks yields four of whatever it now makes.
 */
public final class RandomizeCraftingModifierSource extends EventModifierSource {

    private static final String MODIFIER_ID = "modifier.randomize_crafting";

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) {
            return;
        }
        ItemStack result = event.getInventory().getResult();
        if (result == null || result.isEmpty()) {
            return;
        }
        Modifier modifier = find(player, MODIFIER_ID).orElse(null);
        // With the result hidden the slot keeps the recipe's own item, so the
        // preview gives nothing away; the swap happens as it is taken.
        if (modifier == null || ModifierParams.bool(modifier, "hide_result", false)) {
            return;
        }
        event.getInventory().setResult(substitute(result, modifier, player));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraft(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        ItemStack taken = event.getCurrentItem();
        if (taken == null || taken.isEmpty()) {
            return;
        }
        Modifier modifier = find(player, MODIFIER_ID).orElse(null);
        // Only the hidden case swaps here. With the switch off the slot already
        // holds the substitute, and swapping again would send it somewhere else.
        if (modifier == null || !ModifierParams.bool(modifier, "hide_result", false)) {
            return;
        }
        event.setCurrentItem(substitute(taken, modifier, player));
    }

    private static ItemStack substitute(ItemStack original, Modifier modifier, Player player) {
        Material swapped = RandomizedItemSubstitution.substituteFor(
                original.getType(), modifier, player.getName());
        return new ItemStack(swapped, original.getAmount());
    }
}
