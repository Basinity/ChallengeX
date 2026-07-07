package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.tool_broke}: a player's held or worn item ran out of durability
 * and broke. It rides the equipped-item-broken callback vanilla fires at the
 * break. The {@code item} parameter matches the item that broke.
 */
@Mixin(LivingEntity.class)
public class ToolBrokeMixin {

    @Inject(method = "onEquippedItemBroken", at = @At("HEAD"))
    private void challengex$onBroken(Item item, EquipmentSlot slot, CallbackInfo info) {
        if ((Object) this instanceof ServerPlayer player) {
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.tool_broke", player.getScoreboardName(),
                    Map.of("item", ParamValue.of(itemId))));
        }
    }
}
