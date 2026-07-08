package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.fabric.modifier.ModifierBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * {@code modifier.keep_inventory}: redirects the boxed keepInventory gamerule
 * read {@link Player#dropEquipment} and {@link Player#getBaseExperienceReward}
 * each make to also come back true when the dying player carries the
 * modifier, preserving their inventory and XP the same way the gamerule does,
 * but per player rather than world-wide. The gamerule check in {@code
 * dropEquipment} gates only the general inventory drop; equipment dropping
 * happens unconditionally one call earlier and is untouched.
 */
@Mixin(Player.class)
public class KeepInventoryMixin {

    @Redirect(method = {"dropEquipment", "getBaseExperienceReward"},
            at = @At(value = "INVOKE", target = "Ljava/lang/Boolean;booleanValue()Z"))
    private boolean challengex$gateKeepInventory(Boolean keepInventoryGamerule) {
        if (keepInventoryGamerule) {
            return true;
        }
        return (Object) this instanceof ServerPlayer player
                && ModifierBridge.isActive(player.getScoreboardName(), "modifier.keep_inventory");
    }
}
