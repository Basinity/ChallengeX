package com.basinity.challengex.fabric.mixin;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.fabric.trigger.MixinTriggerBridge;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trigger.game_beaten}: a player beat the game, reaching the end-credits
 * screen after leaving the End through the exit portal. It rides the credits
 * roll, the vanilla marker for finishing the game, and is what the beat-game
 * goal is built on.
 */
@Mixin(ServerPlayer.class)
public class GameBeatMixin {

    @Inject(method = "showEndCredits", at = @At("HEAD"))
    private void challengex$onBeat(CallbackInfo info) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        MixinTriggerBridge.emit(GameEvent.of("trigger.game_beaten", player.getScoreboardName()));
    }
}
