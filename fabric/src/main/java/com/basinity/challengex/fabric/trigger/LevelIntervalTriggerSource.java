package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.level_interval}: fires every {@code level} experience levels,
 * so a value of 5 fires at 5, 10, 15, and so on. It watches the intervals some
 * rule configures and fires when the player's level crosses a fresh multiple of
 * one, once per crossing regardless of how many levels arrived at once.
 */
public final class LevelIntervalTriggerSource extends PlayerPollTriggerSource<Integer> {

    private static final String TRIGGER_ID = "trigger.level_interval";

    @Override
    protected Integer read(ServerPlayer player) {
        return player.experienceLevel;
    }

    @Override
    protected void onChange(ServerPlayer player, Integer previous, Integer current, TriggerContext context) {
        if (current <= previous) {
            return;
        }
        for (ParamValue configured : context.configured(TRIGGER_ID, "level")) {
            long interval = CatalogBounds.clampLong(TRIGGER_ID, "level", TriggerParams.integer(configured));
            if (Math.floorDiv(current, interval) > Math.floorDiv(previous, interval)) {
                context.emit(GameEvent.of(TRIGGER_ID, player.getScoreboardName(),
                        Map.of("level", configured)));
            }
        }
    }
}
