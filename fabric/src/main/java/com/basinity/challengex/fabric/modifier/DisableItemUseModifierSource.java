package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * {@code modifier.disable_item_use}: blocks right-click item use outright for
 * as long as the modifier is active, by failing the interaction rather than
 * detecting and penalizing it after the fact. The optional {@code item}
 * parameter restricts this to one item; omitting it blocks any item, matching
 * how {@code trigger.item_used}'s own {@code item} parameter works. Rides the
 * same {@link UseItemCallback} that source observes.
 */
public final class DisableItemUseModifierSource implements ModifierSource {

    @Override
    public void register(ModifierContext context) {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }
            Modifier modifier = context.find(serverPlayer.getScoreboardName(), "modifier.disable_item_use")
                    .orElse(null);
            if (modifier == null) {
                return InteractionResult.PASS;
            }
            String restrictTo = ModifierParams.string(modifier, "item");
            if (restrictTo == null) {
                return InteractionResult.FAIL;
            }
            ItemStack held = serverPlayer.getItemInHand(hand);
            String itemId = held.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            return restrictTo.equals(itemId) ? InteractionResult.FAIL : InteractionResult.PASS;
        });
    }
}
