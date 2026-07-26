package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** {@code effect.replace_held_random}: swaps each target's held item for a random one. */
public final class ReplaceHeldRandomHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        List<Material> pool = GameIds.allItems();
        if (pool.isEmpty()) {
            return;
        }
        for (Player target : targets) {
            Material item = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            target.getInventory().setItemInMainHand(new ItemStack(item));
        }
    }
}
