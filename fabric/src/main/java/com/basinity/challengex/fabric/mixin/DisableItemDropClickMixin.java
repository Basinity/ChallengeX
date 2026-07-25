package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ModifierBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code modifier.disable_item_drop}, the half of it that happens with a screen
 * open: throwing a stack out of a slot (the drop key inside an inventory) and
 * dropping the carried stack by clicking outside the window. The drop key with no
 * screen open is gated in {@link ItemDropMixin}.
 *
 * <p>Both are cancelled at the head of the click, before vanilla takes anything
 * out of a slot or clears the cursor, so a blocked click leaves the stack exactly
 * where it was rather than deleting it. Only those two click shapes are gated:
 * every other input either moves items between slots or, in the quick-craft
 * drag's case, uses the same outside-the-window slot index for its own begin and
 * end markers, which must keep working. The client predicts the click and the
 * server's own handler resyncs whatever the prediction got wrong, so a blocked
 * click needs no resync of its own.
 *
 * <p>Closing a screen while carrying a stack is not a drop path in 26.2: vanilla
 * puts the carried stack back into the inventory of a player who is still
 * connected and alive, and only drops it for one who is not.
 */
@Mixin(AbstractContainerMenu.class)
public class DisableItemDropClickMixin {

    @Inject(method = "doClick", at = @At("HEAD"), cancellable = true)
    private void challengex$blockContainerDrop(int slotIndex, int button, ContainerInput input, Player player,
            CallbackInfo info) {
        boolean throwFromSlot = input == ContainerInput.THROW;
        boolean dropCarriedOutside = input == ContainerInput.PICKUP
                && slotIndex == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE;
        if (!throwFromSlot && !dropCarriedOutside) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer
                && ModifierBridge.isActive(serverPlayer.getScoreboardName(), "modifier.disable_item_drop")) {
            info.cancel();
        }
    }
}
