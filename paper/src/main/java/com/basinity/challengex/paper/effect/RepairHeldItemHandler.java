package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * {@code effect.repair_held_item}: mends the main-hand item. With an
 * {@code amount} it restores that much durability; without one it fully repairs.
 * A non-damageable item is left alone.
 */
public final class RepairHeldItemHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        boolean hasAmount = EffectParams.has(command, "amount");
        int amount = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", 0));
        for (Player target : targets) {
            ItemStack held = target.getInventory().getItemInMainHand();
            if (!(held.getItemMeta() instanceof Damageable damageable) || !damageable.hasDamage()) {
                // Nothing damaged to mend: either it cannot take damage at all,
                // or it is already whole.
                continue;
            }
            damageable.setDamage(hasAmount ? Math.max(0, damageable.getDamage() - amount) : 0);
            held.setItemMeta(damageable);
        }
    }
}
