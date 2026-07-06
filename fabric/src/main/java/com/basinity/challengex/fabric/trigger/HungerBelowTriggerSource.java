package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.hunger_below}: a player's food level dropped below a number of
 * points, on the vanilla 20-point scale the hunger bar draws as ten shanks. It
 * fires on the way down across the threshold, once per fall; eating back above
 * it re-arms the trigger.
 */
public final class HungerBelowTriggerSource extends PlayerPollTriggerSource<Integer> {

    private static final String TRIGGER_ID = "trigger.hunger_below";

    @Override
    protected Integer read(ServerPlayer player) {
        return player.getFoodData().getFoodLevel();
    }

    @Override
    protected void onChange(ServerPlayer player, Integer previous, Integer current, TriggerContext context) {
        for (ParamValue configured : context.configured(TRIGGER_ID, "points")) {
            long threshold = TriggerParams.integer(configured);
            if (previous >= threshold && current < threshold) {
                context.emit(GameEvent.of(TRIGGER_ID, player.getScoreboardName(),
                        Map.of("points", configured)));
            }
        }
    }
}
