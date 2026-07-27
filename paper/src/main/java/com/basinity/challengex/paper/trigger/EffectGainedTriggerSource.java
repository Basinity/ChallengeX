package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;

/**
 * {@code trigger.effect_gained}: a player gained a status effect, from a potion,
 * a beacon, a mob attack, or any other source. The {@code effect} parameter
 * matches the effect gained; omitting it fires on any.
 */
public final class EffectGainedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEffectGained(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // Only actually gaining one counts: this event also reports effects
        // running out and being cleared.
        EntityPotionEffectEvent.Action action = event.getAction();
        if (action != EntityPotionEffectEvent.Action.ADDED
                && action != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }
        PotionEffect gained = event.getNewEffect();
        if (gained == null) {
            return;
        }
        context().emit(GameEvent.of("trigger.effect_gained", player.getName(),
                Map.of("effect", ParamValue.of(gained.getType().getKey().toString()))));
    }
}
