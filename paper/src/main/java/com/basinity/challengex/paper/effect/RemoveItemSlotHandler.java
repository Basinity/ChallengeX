package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** {@code effect.remove_item_slot}: deletes the item in each target's selected hotbar slot. */
public final class RemoveItemSlotHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        for (Player target : targets) {
            target.getInventory().setItemInMainHand(ItemStack.empty());
        }
    }
}
