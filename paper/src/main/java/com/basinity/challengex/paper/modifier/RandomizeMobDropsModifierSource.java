package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * {@code modifier.randomize_mob_drops}: what a killed mob drops is swapped for
 * a different item, the same substitute every time for a given seed.
 *
 * <p>Scoped to whoever killed it, so a mob that died to fall damage or an
 * explosion drops normally rather than being randomized under a guessed player,
 * which is the same call the Fabric side makes.
 */
public final class RandomizeMobDropsModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMobDrops(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        Modifier modifier = find(killer, "modifier.randomize_mob_drops").orElse(null);
        if (modifier == null) {
            return;
        }
        List<ItemStack> drops = event.getDrops();
        for (int i = 0; i < drops.size(); i++) {
            ItemStack original = drops.get(i);
            if (original == null || original.isEmpty()) {
                continue;
            }
            Material substitute = RandomizedItemSubstitution.substituteFor(
                    original.getType(), modifier, killer.getName());
            drops.set(i, new ItemStack(substitute, original.getAmount()));
        }
    }
}
