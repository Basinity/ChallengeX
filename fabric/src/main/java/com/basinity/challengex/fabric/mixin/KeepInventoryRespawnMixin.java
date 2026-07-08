package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ModifierBridge;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code modifier.keep_inventory}, the other half of it: {@link
 * ServerPlayer#restoreFrom} is what actually copies the dying player's
 * inventory and XP into the freshly respawned player entity. {@link
 * KeepInventoryMixin}'s redirects on {@code Player#dropEquipment} and {@code
 * getBaseExperienceReward} only stop items and XP orbs from spilling onto the
 * ground at the death location; without this one too, blocking the drop
 * without also telling {@code restoreFrom} to copy the inventory over just
 * makes the items vanish; neither dropped nor kept.
 */
@Mixin(ServerPlayer.class)
public class KeepInventoryRespawnMixin {

    @Redirect(method = "restoreFrom", at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;booleanValue()Z"))
    private boolean challengex$gateKeepInventoryOnRespawn(Boolean keepInventoryGamerule, ServerPlayer oldPlayer) {
        if (keepInventoryGamerule) {
            return true;
        }
        return ModifierBridge.isActive(oldPlayer.getScoreboardName(), "modifier.keep_inventory");
    }
}
