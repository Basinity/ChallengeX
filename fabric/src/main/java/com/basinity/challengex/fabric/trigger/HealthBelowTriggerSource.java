package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.health_below}: a player's health dropped below a number of
 * hearts. It fires on the way down across the threshold, once per fall, not
 * every tick spent under it; healing back above it re-arms the trigger.
 */
public final class HealthBelowTriggerSource extends PlayerPollTriggerSource<Double> {

    private static final String TRIGGER_ID = "trigger.health_below";
    private static final double HALF_HEARTS_PER_HEART = 2.0;

    @Override
    protected Double read(ServerPlayer player) {
        return player.getHealth() / HALF_HEARTS_PER_HEART;
    }

    @Override
    protected void onChange(ServerPlayer player, Double previous, Double current, TriggerContext context) {
        for (ParamValue configured : context.configured(TRIGGER_ID, "hearts")) {
            double threshold = TriggerParams.decimal(configured);
            if (previous >= threshold && current < threshold) {
                context.emit(GameEvent.of(TRIGGER_ID, player.getScoreboardName(),
                        Map.of("hearts", configured)));
            }
        }
    }
}
