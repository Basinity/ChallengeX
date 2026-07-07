package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.enchantment_applied}: a player enchanted an item at an
 * enchanting table. It rides the enchant callback vanilla runs once the table
 * applies the enchantments, and fires once per enchantment the result carries,
 * so a filter on any applied enchantment matches. The {@code enchantment} and
 * {@code level} parameters match that enchantment and its level.
 */
@Mixin(ServerPlayer.class)
public class EnchantmentAppliedMixin {

    @Inject(method = "onEnchantmentPerformed", at = @At("TAIL"))
    private void challengex$onEnchant(ItemStack enchanted, int levels, CallbackInfo info) {
        ItemEnchantments enchantments = enchanted.get(DataComponents.ENCHANTMENTS);
        if (enchantments == null || enchantments.isEmpty()) {
            return;
        }
        ServerPlayer player = (ServerPlayer) (Object) this;
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            String enchantmentId = enchantment.unwrapKey()
                    .map(key -> key.identifier().toString()).orElse(null);
            if (enchantmentId == null) {
                continue;
            }
            MixinTriggerBridge.emit(GameEvent.of("trigger.enchantment_applied", player.getScoreboardName(),
                    Map.of("enchantment", ParamValue.of(enchantmentId),
                            "level", ParamValue.of((long) enchantments.getLevel(enchantment)))));
        }
    }
}
