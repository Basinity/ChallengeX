package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trigger.food_eaten}: a player finished eating food. 26.2 routes item
 * consumption through the {@link Consumable} component rather than the old
 * {@code Item.finishUsingItem}, so this rides {@link Consumable#onConsume}. That
 * fires for every consumable (potions, milk, honey), so it is gated to stacks
 * carrying a food component. The {@code item} parameter matches the eaten item's
 * id.
 */
@Mixin(Consumable.class)
public class FoodEatenMixin {

    @Inject(method = "onConsume", at = @At("HEAD"))
    private void challengex$onEaten(Level level, LivingEntity entity, ItemStack stack,
            CallbackInfoReturnable<ItemStack> info) {
        if (!(entity instanceof ServerPlayer player) || !stack.has(DataComponents.FOOD)) {
            return;
        }
        String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        MixinTriggerBridge.emit(GameEvent.of("trigger.food_eaten", player.getScoreboardName(),
                Map.of("item", ParamValue.of(itemId))));
    }
}
