package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.level_reached}: a player's experience level crossed up to the
 * configured {@code level}. Like the other threshold triggers it watches the
 * levels some rule configures and fires once on reaching each, re-arming if the
 * player drops back below.
 */
public final class LevelReachedTriggerSource extends PlayerPollTriggerSource<Integer> {

    private static final String TRIGGER_ID = "trigger.level_reached";

    @Override
    protected Integer read(ServerPlayer player) {
        return player.experienceLevel;
    }

    @Override
    protected void onChange(ServerPlayer player, Integer previous, Integer current, TriggerContext context) {
        for (ParamValue configured : context.configured(TRIGGER_ID, "level")) {
            long level = TriggerParams.integer(configured);
            if (previous < level && current >= level) {
                context.emit(GameEvent.of(TRIGGER_ID, player.getScoreboardName(),
                        Map.of("level", configured)));
            }
        }
    }
}
