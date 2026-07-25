package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * {@code effect.give_random_item}: gives each target a randomly chosen item.
 *
 * <p>How many arrives is a choice. Left alone the amount is random too, drawn
 * against what one stack of the item that came up actually holds, so an item
 * stacking to sixteen never gives more than sixteen and one that does not stack
 * gives exactly one. Switch {@code fixed_amount} on and the {@code amount}
 * parameter sets the number instead. The item itself is always random, which is
 * the point of the effect.
 */
public final class GiveRandomItemHandler implements EffectHandler {


    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        boolean fixed = EffectParams.bool(command, "fixed_amount", false);
        int amount = CatalogBounds.clampInt(command.effectId(), "amount",
                EffectParams.integer(command, "amount", 1));
        for (ServerPlayer target : targets) {
            BuiltInRegistries.ITEM.getRandom(target.getRandom()).ifPresent(item -> {
                int perStack = Math.max(1, item.value().getDefaultMaxStackSize());
                // Rolled per target and against this item's own stack, so two
                // players are neither handed the same number nor handed more of
                // something than a stack of it holds.
                int wanted = fixed ? amount : 1 + target.getRandom().nextInt(perStack);
                // A fixed amount can ask for more than one stack of the item
                // that came up, so it arrives as several, the way the vanilla
                // give command hands them over.
                while (wanted > 0) {
                    int inThisStack = Math.min(wanted, perStack);
                    target.addItem(new ItemStack(item, inThisStack));
                    wanted -= inThisStack;
                }
            });
        }
    }
}
