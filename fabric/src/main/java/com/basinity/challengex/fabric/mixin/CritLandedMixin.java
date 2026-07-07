package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.crit_landed}: a player landed a critical hit. It rides the
 * vanilla crit call, which fires exactly when an attack is judged a crit.
 */
@Mixin(Player.class)
public class CritLandedMixin {

    @Inject(method = "crit", at = @At("HEAD"))
    private void challengex$onCrit(Entity target, CallbackInfo info) {
        if ((Object) this instanceof ServerPlayer player) {
            MixinTriggerBridge.emit(GameEvent.of("trigger.crit_landed", player.getScoreboardName()));
        }
    }
}
