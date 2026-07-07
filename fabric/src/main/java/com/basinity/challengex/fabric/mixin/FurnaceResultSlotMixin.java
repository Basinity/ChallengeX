package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.item_smelted}: a player took a smelted result out of a furnace,
 * blast furnace, or smoker. It rides the furnace result slot's take, so it fires
 * on collecting the output, not on the smelt finishing in the slot. The
 * {@code item} parameter matches the smelted item's id.
 */
@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {

    @Inject(method = "onTake", at = @At("TAIL"))
    private void challengex$onSmelted(Player player, ItemStack stack, CallbackInfo info) {
        if (player instanceof ServerPlayer serverPlayer && !stack.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.item_smelted", serverPlayer.getScoreboardName(),
                    Map.of("item", ParamValue.of(itemId))));
        }
    }
}
