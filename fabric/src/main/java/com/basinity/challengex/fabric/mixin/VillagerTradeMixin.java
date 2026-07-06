package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.villager_traded}: a player completed a trade with a villager or
 * wandering trader. It rides the merchant result slot's take, which fires when
 * the player collects the traded-for item, the point the trade is finalized.
 */
@Mixin(MerchantResultSlot.class)
public class VillagerTradeMixin {

    @Inject(method = "onTake", at = @At("TAIL"))
    private void challengex$onTrade(Player player, ItemStack stack, CallbackInfo info) {
        if (player instanceof ServerPlayer serverPlayer) {
            MixinTriggerBridge.emit(GameEvent.of("trigger.villager_traded", serverPlayer.getScoreboardName()));
        }
    }
}
