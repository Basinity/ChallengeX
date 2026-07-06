package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.damage_dealt}: a player dealt damage to something. It fires on
 * the damage landing, whether or not the target survives it.
 */
public final class DamageDealtTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (source.getEntity() instanceof ServerPlayer attacker) {
                context.emit(GameEvent.of("trigger.damage_dealt", attacker.getScoreboardName()));
            }
        });
    }
}
