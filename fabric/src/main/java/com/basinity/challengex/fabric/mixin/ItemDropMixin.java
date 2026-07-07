package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.item_dropped}: a player tossed an item out of their inventory.
 * It rides the player's own drop action (the drop key), which is the deliberate
 * toss, so death drops and other inventory spills do not trigger it. The
 * {@code item} parameter matches the tossed item; pressing the key with an empty
 * hand drops nothing, so it fires only when the held stack has something in it.
 */
@Mixin(ServerPlayer.class)
public class ItemDropMixin {

    @Inject(method = "drop(Z)V", at = @At("HEAD"))
    private void challengex$onDrop(boolean dropEntireStack, CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ItemStack held = player.getInventory().getSelectedItem();
        if (!held.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.item_dropped", player.getScoreboardName(),
                    Map.of("item", ParamValue.of(itemId))));
        }
    }
}
