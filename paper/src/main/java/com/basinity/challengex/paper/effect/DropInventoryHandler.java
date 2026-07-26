package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;

/** {@code effect.drop_inventory}: drops each target's entire inventory on the ground. */
public final class DropInventoryHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        for (Player target : targets) {
            PlayerInventory inventory = target.getInventory();
            for (int slot = 0; slot < inventory.getSize(); slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                target.getWorld().dropItemNaturally(target.getLocation(), stack);
                inventory.setItem(slot, null);
            }
        }
    }
}
