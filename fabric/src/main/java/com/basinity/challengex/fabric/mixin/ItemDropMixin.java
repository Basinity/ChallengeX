package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.modifier.ModifierBridge;
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
 * {@code trigger.item_dropped} and {@code modifier.disable_item_drop}, which
 * both ride the player's own drop action (the drop key). That action is the
 * deliberate toss, so death drops and other inventory spills neither trigger nor
 * are blocked here. The trigger's {@code item} parameter matches the tossed
 * item; pressing the key with an empty hand drops nothing, so it fires only when
 * the held stack has something in it.
 *
 * <p>The modifier cancels the action before vanilla takes the stack out of the
 * selected slot, so the item stays in the inventory rather than being deleted,
 * and the trigger stays silent for a drop that never happened. The two share one
 * handler because they share an injection point and the order they run in has to
 * be certain. The client removes the item from its own copy of the inventory as
 * soon as the key is pressed, so a blocked drop resyncs the open menu to put it
 * back on screen. The container drop paths are gated in
 * {@link DisableItemDropClickMixin}.
 */
@Mixin(ServerPlayer.class)
public class ItemDropMixin {

    @Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
    private void challengex$onDrop(boolean dropEntireStack, CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (ModifierBridge.isActive(player.getScoreboardName(), "modifier.disable_item_drop")) {
            player.containerMenu.sendAllDataToRemote();
            info.cancel();
            return;
        }
        ItemStack held = player.getInventory().getSelectedItem();
        if (!held.isEmpty()) {
            String itemId = BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
            MixinTriggerBridge.emit(GameEvent.of("trigger.item_dropped", player.getScoreboardName(),
                    Map.of("item", ParamValue.of(itemId))));
        }
    }
}
