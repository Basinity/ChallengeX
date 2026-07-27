package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.core.model.Modifier;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * {@code modifier.disable_interaction}: blocks right-click interaction with one
 * specific block (its required {@code target} id, for a challenge like "no
 * crafting table") for as long as the modifier is active.
 */
public final class DisableInteractionModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Modifier modifier = find(event.getPlayer(), "modifier.disable_interaction").orElse(null);
        if (modifier == null) {
            return;
        }
        String target = ModifierParams.string(modifier, "target");
        if (block.getType().getKey().toString().equals(target)) {
            event.setCancelled(true);
        }
    }
}
