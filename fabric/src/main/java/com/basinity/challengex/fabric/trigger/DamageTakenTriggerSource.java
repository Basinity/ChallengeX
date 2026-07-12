package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.damage_taken}: a player was hit. A hit that lands but deals
 * no damage still counts; a hit blocked by a shield does not (that's {@code
 * trigger.shield_blocked}'s job). The {@code source} parameter matches the
 * damage type id ({@code minecraft:lava}); omitting it fires on any damage.
 */
public final class DamageTakenTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            if (blocked || !(entity instanceof ServerPlayer player)) {
                return;
            }
            context.emit(GameEvent.of("trigger.damage_taken", player.getScoreboardName(),
                    Map.of("source", ParamValue.of(GameIds.of(source)))));
        });
    }
}
