package com.basinity.challengex.fabric.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import net.fabricmc.fabric.api.entity.event.v1.effect.ServerMobEffectEvents;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code trigger.effect_gained}: a player gained a status effect, from a potion,
 * a beacon, a mob attack, or any other source. The {@code effect} parameter
 * matches the effect gained; omitting it fires on any.
 */
public final class EffectGainedTriggerSource implements TriggerSource {

    @Override
    public void register(TriggerContext context) {
        ServerMobEffectEvents.AFTER_ADD.register((instance, entity, effectContext) -> {
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            String effectId = instance.getEffect().unwrapKey()
                    .map(key -> key.identifier().toString()).orElse(null);
            if (effectId != null) {
                context.emit(GameEvent.of("trigger.effect_gained", player.getScoreboardName(),
                        Map.of("effect", ParamValue.of(effectId))));
            }
        });
    }
}
