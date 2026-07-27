package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.common.trigger.TriggerParams;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.Map;
import org.bukkit.entity.Player;

/**
 * {@code trigger.level_interval}: fires every {@code level} experience levels,
 * so a value of 5 fires at 5, 10, 15, and so on. It watches the intervals some
 * rule configures and fires when the player's level crosses a fresh multiple of
 * one, once per crossing regardless of how many levels arrived at once.
 */
public final class LevelIntervalTriggerSource extends PlayerPollTriggerSource<Integer> {

    private static final String TRIGGER_ID = "trigger.level_interval";

    @Override
    protected Integer read(Player player) {
        return player.getLevel();
    }

    @Override
    protected void onChange(Player player, Integer previous, Integer current, TriggerContext context) {
        if (current <= previous) {
            return;
        }
        for (ParamValue configured : context.configured(TRIGGER_ID, "level")) {
            long interval = CatalogBounds.clampLong(TRIGGER_ID, "level", TriggerParams.integer(configured));
            if (Math.floorDiv(current, interval) > Math.floorDiv(previous, interval)) {
                context.emit(GameEvent.of(TRIGGER_ID, player.getName(), Map.of("level", configured)));
            }
        }
    }
}
