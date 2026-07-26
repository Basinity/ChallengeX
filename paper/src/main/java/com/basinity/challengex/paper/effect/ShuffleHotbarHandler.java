package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** {@code effect.shuffle_hotbar}: randomly reorders each target's nine hotbar slots. */
public final class ShuffleHotbarHandler implements EffectHandler {

    private static final int HOTBAR_SIZE = 9;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        for (Player target : targets) {
            PlayerInventory inventory = target.getInventory();
            List<ItemStack> slots = new ArrayList<>(HOTBAR_SIZE);
            for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
                slots.add(inventory.getItem(slot));
            }
            Collections.shuffle(slots);
            for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
                inventory.setItem(slot, slots.get(slot));
            }
        }
    }
}
