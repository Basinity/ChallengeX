package com.basinity.challengex.paper.trigger;

import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Whether a hit was stopped by a shield. Both damage triggers exclude blocked
 * hits and the shield trigger wants exactly them, so the test lives in one
 * place rather than being written three ways.
 *
 * <p>Bukkit reports blocking as a negative damage modifier rather than as a
 * flag, which is why this reads the way it does.
 */
final class Blocking {

    private Blocking() {
    }

    @SuppressWarnings("deprecation")
    static boolean wasBlocked(EntityDamageEvent event) {
        return event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)
                && event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) != 0.0;
    }
}
