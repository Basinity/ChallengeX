package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.enchantment_applied}: a player enchanted an item at an
 * enchanting table. It rides the enchant callback vanilla runs once the table
 * charges the levels and applies the enchantment.
 */
@Mixin(ServerPlayer.class)
public class EnchantmentAppliedMixin {

    @Inject(method = "onEnchantmentPerformed", at = @At("TAIL"))
    private void challengex$onEnchant(ItemStack enchanted, int levels, CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        MixinTriggerBridge.emit(GameEvent.of("trigger.enchantment_applied", player.getScoreboardName()));
    }
}
