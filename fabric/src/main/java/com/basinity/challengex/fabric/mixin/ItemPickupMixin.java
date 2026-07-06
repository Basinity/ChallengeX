package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.item_picked_up}: a player picked an item up off the ground. It
 * rides the living-entity pickup callback, which vanilla fires once the item is
 * actually taken into the inventory, so a full inventory that leaves the item on
 * the ground never triggers it. The {@code item} parameter matches the picked
 * item's id.
 */
@Mixin(LivingEntity.class)
public class ItemPickupMixin {

    @Inject(method = "onItemPickup", at = @At("TAIL"))
    private void challengex$onPickup(ItemEntity itemEntity, CallbackInfo info) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString();
        MixinTriggerBridge.emit(GameEvent.of("trigger.item_picked_up", player.getScoreboardName(),
                Map.of("item", ParamValue.of(itemId))));
    }
}
