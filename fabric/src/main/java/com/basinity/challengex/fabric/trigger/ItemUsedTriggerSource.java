package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

/**
 * {@code trigger.item_used}: a player right-clicked to use a held item. The
 * {@code item} parameter matches the used item; omitting it fires on any use.
 * Using with an empty hand is not a use, so an empty hand never fires it.
 */
public final class ItemUsedTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack held = serverPlayer.getItemInHand(hand);
                if (!held.isEmpty()) {
                    String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
                    context.emit(GameEvent.of("trigger.item_used", serverPlayer.getScoreboardName(),
                            Map.of("item", ParamValue.of(itemId))));
                }
            }
            return InteractionResult.PASS;
        });
    }
}
