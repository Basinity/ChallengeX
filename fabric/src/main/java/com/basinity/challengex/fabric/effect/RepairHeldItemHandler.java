package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * {@code effect.repair_held_item}: mends the main-hand item. With an
 * {@code amount} it restores that much durability; without one it fully repairs.
 * A non-damageable item is left alone.
 */
public final class RepairHeldItemHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        boolean hasAmount = EffectParams.has(command, "amount");
        int amount = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", 0));
        for (ServerPlayer target : targets) {
            ItemStack held = target.getMainHandItem();
            if (!held.isDamageableItem()) {
                continue;
            }
            if (hasAmount) {
                held.setDamageValue(Math.max(0, held.getDamageValue() - amount));
            } else {
                held.setDamageValue(0);
            }
        }
    }
}
