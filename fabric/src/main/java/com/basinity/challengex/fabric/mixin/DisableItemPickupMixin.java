package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ModifierBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code modifier.disable_item_pickup}: cancels the entity-touch callback that
 * takes something lying on the ground into a walking player's inventory. Two
 * entity kinds reach a player's inventory that way and both are gated here, item
 * entities and spent arrows; a thrown trident's own touch handler defers to the
 * arrow one, so it is covered without a third target.
 *
 * <p>Cancelling at the head leaves the entity exactly as it was, since vanilla
 * has not touched the inventory or the item's count yet at that point, and the
 * item stays on the ground for as long as it would have anyway.
 * {@code trigger.item_picked_up} rides the callback vanilla fires only once a
 * pickup has actually gone through, so it stays silent here with no check of its
 * own.
 */
@Mixin({ItemEntity.class, AbstractArrow.class})
public class DisableItemPickupMixin {

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void challengex$blockPickup(Player player, CallbackInfo info) {
        if (player instanceof ServerPlayer serverPlayer
                && ModifierBridge.isActive(serverPlayer.getScoreboardName(), "modifier.disable_item_pickup")) {
            info.cancel();
        }
    }
}
