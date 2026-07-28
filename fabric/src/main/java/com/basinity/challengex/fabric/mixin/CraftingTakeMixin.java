package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.fabric.modifier.ModifierBridge;
import com.basinity.challengex.fabric.modifier.RandomizedItemSubstitution;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The hidden half of {@code modifier.randomize_crafting}: with
 * {@code hide_result} on, the result slot keeps showing the recipe's own item
 * and the substitution happens here, as the stack leaves the slot, so the
 * player finds out what they made only once they have made it.
 *
 * <p>With the switch off this does nothing, because the slot already holds the
 * substitute by then and swapping a second time would send it somewhere else
 * again.
 */
@Mixin(ResultSlot.class)
public abstract class CraftingTakeMixin {

    @Shadow
    @Final
    private Player player;

    @Inject(method = "remove", at = @At("RETURN"), cancellable = true)
    private void challengex$swapOnTake(int count, CallbackInfoReturnable<ItemStack> info) {
        ItemStack taken = info.getReturnValue();
        if (taken == null || taken.isEmpty()) {
            return;
        }
        if (!(this.player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Modifier modifier = ModifierBridge
                .find(serverPlayer.getScoreboardName(), "modifier.randomize_crafting")
                .orElse(null);
        if (modifier == null || !ModifierParams.bool(modifier, "hide_result", false)) {
            return;
        }
        Identifier original = BuiltInRegistries.ITEM.getKey(taken.getItem());
        Identifier substitute = RandomizedItemSubstitution.substituteFor(
                original, modifier, serverPlayer.getScoreboardName());
        BuiltInRegistries.ITEM.getOptional(substitute).ifPresent(item ->
                info.setReturnValue(new ItemStack(item, taken.getCount())));
    }
}
