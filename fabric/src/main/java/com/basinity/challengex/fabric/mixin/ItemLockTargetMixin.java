package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ItemLocks;
import java.util.UUID;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code modifier.item_lock} and the items vanilla reserves for one player. An
 * item entity can name the only player allowed to pick it up, which is what a
 * give command does with whatever would not fit in the inventory it was aimed at.
 * Refusing to add a locked item makes exactly that happen, and the item then sits
 * on the ground reserved for the one player who may not have it, so nobody can
 * ever pick it up and it despawns where it fell.
 *
 * <p>The reservation is dropped in that case, leaving an ordinary item anybody
 * else can collect, the holder included. A reservation for a player who is free
 * to take the item is left exactly as vanilla set it.
 */
@Mixin(ItemEntity.class)
public class ItemLockTargetMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void challengex$dropReservationForLockedItem(UUID target, CallbackInfo info) {
        if (ItemLocks.blocked(target, ((ItemEntity) (Object) this).getItem())) {
            info.cancel();
        }
    }
}
