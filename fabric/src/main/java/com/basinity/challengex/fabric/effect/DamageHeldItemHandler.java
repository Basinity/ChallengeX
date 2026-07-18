package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * {@code effect.damage_held_item}: wears down the main-hand item's durability by
 * {@code amount} (default fifty), breaking it if that runs it out. A
 * non-damageable item is left alone.
 */
public final class DamageHeldItemHandler implements EffectHandler {

    private static final int DEFAULT_AMOUNT = 50;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int amount = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", DEFAULT_AMOUNT));
        for (ServerPlayer target : targets) {
            ItemStack held = target.getMainHandItem();
            if (held.isDamageableItem()) {
                held.hurtAndBreak(amount, target, EquipmentSlot.MAINHAND);
            }
        }
    }
}
