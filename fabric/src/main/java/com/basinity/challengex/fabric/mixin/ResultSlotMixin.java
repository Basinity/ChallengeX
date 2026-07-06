package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.item_crafted}: a player took a crafted result out of a crafting
 * grid. It rides the crafting result slot's take, which fires once the item is
 * actually crafted and picked up. The {@code item} parameter matches the crafted
 * item's id.
 */
@Mixin(ResultSlot.class)
public class ResultSlotMixin {

    @Inject(method = "onTake", at = @At("TAIL"))
    private void challengex$onCrafted(Player player, ItemStack stack, CallbackInfo info) {
        if (!(player instanceof ServerPlayer serverPlayer) || stack.isEmpty()) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        MixinTriggerBridge.emit(GameEvent.of("trigger.item_crafted", serverPlayer.getScoreboardName(),
                Map.of("item", ParamValue.of(itemId))));
    }
}
