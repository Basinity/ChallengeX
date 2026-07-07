package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.started_gliding}: a player began elytra flight. It fires on the
 * rising edge of fall-flying, once per glide, not every tick spent airborne.
 */
public final class StartedGlidingTriggerSource extends PlayerPollTriggerSource<Boolean> {

    @Override
    protected Boolean read(ServerPlayer player) {
        return player.isFallFlying();
    }

    @Override
    protected void onChange(ServerPlayer player, Boolean previous, Boolean current, TriggerContext context) {
        if (current) {
            context.emit(GameEvent.of("trigger.started_gliding", player.getScoreboardName()));
        }
    }
}
