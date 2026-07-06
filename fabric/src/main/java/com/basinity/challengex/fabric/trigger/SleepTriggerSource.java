package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.slept}: a player started sleeping. It fires on getting into
 * the bed, not on the night passing, so a refused sleep never reaches it.
 */
public final class SleepTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        EntitySleepEvents.START_SLEEPING.register((entity, pos) -> {
            if (entity instanceof ServerPlayer player) {
                context.emit(GameEvent.of("trigger.slept", player.getScoreboardName()));
            }
        });
    }
}
