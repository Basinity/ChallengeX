package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * {@code effect.swap_inventory}: swaps each target's inventory with a random
 * other online player, from any world.
 */
public final class SwapInventoryHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        for (Player target : targets) {
            Player other = Players.randomOther(target, server);
            if (other != null) {
                swap(target.getInventory(), other.getInventory());
            }
        }
    }

    private static void swap(PlayerInventory first, PlayerInventory second) {
        for (int slot = 0; slot < first.getSize(); slot++) {
            ItemStack held = first.getItem(slot);
            first.setItem(slot, second.getItem(slot));
            second.setItem(slot, held);
        }
    }
}
