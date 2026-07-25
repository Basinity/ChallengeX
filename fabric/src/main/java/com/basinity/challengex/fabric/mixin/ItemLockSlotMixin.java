package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ItemLocks;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code modifier.item_lock} on the way out of a slot that is not the player's
 * own. Vanilla asks this question before it takes anything, for a plain click, a
 * shift-click and a throw alike, so answering no leaves the stack sitting where
 * it is. One gate here covers every container the game has: a chest or barrel, a
 * crafting or furnace result, a villager's offer.
 *
 * <p>Slots backed by the player's own inventory are deliberately not gated. A
 * player is never blocked from moving their own items around, and a locked item
 * that somehow got in there is the settling pass's business, not this one's. A
 * crafting grid is left alone for the same reason: what is in it came out of that
 * player's own inventory on its way to becoming something else, and refusing to
 * give it back would strand it there. The result slot the grid feeds is a
 * different matter and stays gated, which is what stops a locked item being
 * crafted. The few slots that answer this question themselves rather than
 * deferring to the base one, an anvil or smithing result, are not covered here.
 */
@Mixin(Slot.class)
public class ItemLockSlotMixin {

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void challengex$refuseLockedItem(Player player, CallbackInfoReturnable<Boolean> info) {
        Slot slot = (Slot) (Object) this;
        if (slot.container instanceof Inventory || slot.container instanceof CraftingContainer) {
            return;
        }
        if (ItemLocks.blocked(player, slot.getItem())) {
            info.setReturnValue(false);
        }
    }
}
