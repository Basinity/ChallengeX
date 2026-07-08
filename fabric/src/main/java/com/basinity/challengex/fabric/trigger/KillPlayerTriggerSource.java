package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.kill_player}: a player killed another player. The {@code name}
 * parameter matches the victim's name; omitting it fires on any player kill.
 * This is the killer side of a player death, distinct from
 * {@code trigger.player_died} (which fires on the victim) and
 * {@code trigger.mob_killed} (which excludes player victims entirely).
 */
public final class KillPlayerTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, cause) -> {
            if (!(entity instanceof ServerPlayer victim)
                    || !(cause.getEntity() instanceof ServerPlayer killer)
                    || killer == victim) {
                return;
            }
            context.emit(GameEvent.of("trigger.kill_player", killer.getScoreboardName(),
                    Map.of("name", ParamValue.of(victim.getScoreboardName()))));
        });
    }
}
