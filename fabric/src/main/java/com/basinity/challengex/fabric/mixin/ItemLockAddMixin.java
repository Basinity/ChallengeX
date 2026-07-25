package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ItemLocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code modifier.item_lock} on the way into an inventory. Reporting that a
 * locked stack did not fit is the one refusal that needs no path of its own for
 * each caller, because every caller of this method already has to cope with a
 * full inventory: an item on the ground stays lying there with its count intact,
 * and a give command drops what it could not hand over. Picking arrows back up
 * runs through here too.
 */
@Mixin(Inventory.class)
public class ItemLockAddMixin {

    @Inject(method = "add(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void challengex$refuseLockedItem(ItemStack stack, CallbackInfoReturnable<Boolean> info) {
        if (ItemLocks.blocked(((Inventory) (Object) this).player, stack)) {
            info.setReturnValue(false);
        }
    }
}
