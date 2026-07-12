package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.damage_dealt}: a player dealt damage to something. It fires on
 * the damage landing, whether or not the target survives it. A hit that lands
 * but deals no damage still counts; a hit the target blocked with a shield does
 * not, mirroring {@code trigger.damage_taken}, so neither side of a blocked hit
 * fires. The {@code source} parameter matches the damage type dealt ({@code
 * minecraft:player_attack}) and {@code target} matches the entity that was hit;
 * either omitted matches anything.
 */
public final class DamageDealtTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (!blocked && source.getEntity() instanceof ServerPlayer attacker) {
                String targetId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
                context.emit(GameEvent.of("trigger.damage_dealt", attacker.getScoreboardName(),
                        Map.of("source", ParamValue.of(GameIds.of(source)),
                                "target", ParamValue.of(targetId))));
            }
        });
    }
}
