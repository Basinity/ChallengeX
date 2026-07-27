package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.common.trigger.TriggerParams;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;

/**
 * {@code trigger.height_crossed}: a player crossed a Y level, in either
 * direction. The {@code y} parameter is the level watched rather than the
 * player's position, so the source reads the configured levels and detects the
 * crossing itself.
 */
public final class HeightCrossedTriggerSource extends PlayerPollTriggerSource<Integer> {

    private static final String TRIGGER_ID = "trigger.height_crossed";

    @Override
    protected Integer read(Player player) {
        return player.getLocation().getBlockY();
    }

    @Override
    protected void onChange(Player player, Integer previous, Integer current, TriggerContext context) {
        for (ParamValue configured : context.configured(TRIGGER_ID, "y")) {
            long level = TriggerParams.integer(configured);
            if (crossed(previous, current, level)) {
                context.emit(GameEvent.of(TRIGGER_ID, player.getName(), Map.of("y", configured)));
            }
        }
    }

    /** Whether the level sits between the two positions, whichever way it was passed. */
    private static boolean crossed(int previous, int current, long level) {
        return (previous < level && current >= level) || (previous >= level && current < level);
    }
}
