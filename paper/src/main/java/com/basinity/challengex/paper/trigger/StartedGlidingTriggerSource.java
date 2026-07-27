package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.entity.Player;

/**
 * {@code trigger.started_gliding}: a player began elytra flight. It fires on the
 * rising edge of gliding, once per glide, not every tick spent airborne.
 */
public final class StartedGlidingTriggerSource extends PlayerPollTriggerSource<Boolean> {

    @Override
    protected Boolean read(Player player) {
        return player.isGliding();
    }

    @Override
    protected void onChange(Player player, Boolean previous, Boolean current, TriggerContext context) {
        if (current) {
            context.emit(GameEvent.of("trigger.started_gliding", player.getName()));
        }
    }
}
