package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.player_died}: a player died. Dying carries no built-in run
 * meaning; a death-ends-the-run challenge pairs this trigger with the
 * lose-challenge effect like any other rule.
 */
public final class PlayerDeathTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, cause) -> {
            if (entity instanceof ServerPlayer player) {
                context.emit(GameEvent.of("trigger.player_died", player.getScoreboardName()));
            }
        });
    }
}
