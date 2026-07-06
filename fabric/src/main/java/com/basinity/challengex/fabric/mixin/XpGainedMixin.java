package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.xp_gained}: a player gained experience points, from a furnace
 * pickup, mob kill, ore, breeding, and the rest. It rides the point award, so a
 * bare level-set command that hands out no points does not trigger it.
 */
@Mixin(ServerPlayer.class)
public class XpGainedMixin {

    @Inject(method = "giveExperiencePoints", at = @At("TAIL"))
    private void challengex$onXp(int points, CallbackInfo info) {
        if (points > 0) {
            ServerPlayer player = (ServerPlayer) (Object) this;
            MixinTriggerBridge.emit(GameEvent.of("trigger.xp_gained", player.getScoreboardName()));
        }
    }
}
