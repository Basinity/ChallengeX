package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * {@code trigger.mob_killed}: a player killed a mob. The {@code mob} parameter
 * matches the mob's entity type id. Players dying is
 * {@code trigger.player_died} instead, so player kills are not mob kills.
 */
public final class MobKilledTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, cause) -> {
            if (entity instanceof Player || !(cause.getEntity() instanceof ServerPlayer killer)) {
                return;
            }
            String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            context.emit(GameEvent.of("trigger.mob_killed", killer.getScoreboardName(),
                    Map.of("mob", ParamValue.of(mobId))));
        });
    }
}
