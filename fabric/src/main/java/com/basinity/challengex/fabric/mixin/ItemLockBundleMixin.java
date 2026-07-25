package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ItemLocks;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code modifier.item_lock} on the way out of a bundle. A bundle's contents do
 * not hold a lock, so an item can be stashed in one freely, but pulling a locked
 * one back out has to fail, and it is the one acquisition that reaches an
 * inventory without passing a container slot or the inventory's own add.
 *
 * <p>Both of a bundle's click handlers take their item out through the same call,
 * which is what this redirects. Rather than working out in advance which item a
 * bundle would hand over, and so having to mirror how it picks one, the item is
 * taken out and put straight back when it turns out to be locked. Vanilla is then
 * told nothing came out, which is what it reports for an empty bundle: the
 * contents it writes back are the ones it started with, and neither the cursor
 * nor the slot receives anything.
 */
@Mixin(net.minecraft.world.item.BundleItem.class)
public class ItemLockBundleMixin {

    private static final String REMOVE_ONE =
            "Lnet/minecraft/world/item/component/BundleContents$Mutable;removeOne()Lnet/minecraft/world/item/ItemStack;";

    @Redirect(method = "overrideStackedOnOther", at = @At(value = "INVOKE", target = REMOVE_ONE))
    private ItemStack challengex$gateTakeIntoSlot(BundleContents.Mutable contents, ItemStack bundle, Slot slot,
            ClickAction action, Player player) {
        return challengex$removeUnlessLocked(contents, player);
    }

    @Redirect(method = "overrideOtherStackedOnMe", at = @At(value = "INVOKE", target = REMOVE_ONE))
    private ItemStack challengex$gateTakeOntoCursor(BundleContents.Mutable contents, ItemStack bundle, ItemStack other,
            Slot slot, ClickAction action, Player player, SlotAccess access) {
        return challengex$removeUnlessLocked(contents, player);
    }

    private ItemStack challengex$removeUnlessLocked(BundleContents.Mutable contents, Player player) {
        ItemStack removed = contents.removeOne();
        if (removed == null || !ItemLocks.blocked(player, removed)) {
            return removed;
        }
        contents.tryInsert(removed);
        return null;
    }
}
