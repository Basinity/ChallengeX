package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * {@code effect.damage_held_item}: wears down the main-hand item's durability by
 * {@code amount} (default fifty), breaking it if that runs it out. A
 * non-damageable item is left alone.
 */
public final class DamageHeldItemHandler implements EffectHandler {

    private static final int DEFAULT_AMOUNT = 50;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        int amount = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", DEFAULT_AMOUNT));
        for (Player target : targets) {
            ItemStack held = target.getInventory().getItemInMainHand();
            // damage() is the API that also handles unbreaking and the break
            // itself, so a worn-out tool snaps the way it would in play.
            held.damage(amount, target);
        }
    }
}
