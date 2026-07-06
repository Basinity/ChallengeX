package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.sneaked}: a player started sneaking. It fires on the way into a
 * sneak, not once per tick spent sneaking, so holding the key is one event.
 */
public final class SneakTriggerSource extends PlayerPollTriggerSource<Boolean> {

    @Override
    protected Boolean read(ServerPlayer player) {
        return player.isShiftKeyDown();
    }

    @Override
    protected void onChange(ServerPlayer player, Boolean previous, Boolean current, TriggerContext context) {
        if (current) {
            context.emit(GameEvent.of("trigger.sneaked", player.getScoreboardName()));
        }
    }
}
