package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;

/**
 * {@code trigger.dimension_changed}: a player arrived in a dimension. The
 * {@code dimension} parameter matches the destination's id
 * ({@code minecraft:the_nether}), not the one departed.
 */
public final class DimensionChangedTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            String dimensionId = destination.dimension().identifier().toString();
            context.emit(GameEvent.of("trigger.dimension_changed", player.getScoreboardName(),
                    Map.of("dimension", ParamValue.of(dimensionId))));
        });
    }
}
