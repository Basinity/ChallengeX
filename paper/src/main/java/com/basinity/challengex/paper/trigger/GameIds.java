package com.basinity.challengex.paper.trigger;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;

/**
 * Turns game objects into the namespaced string ids that trigger parameters
 * match against, so a preset names a damage source {@code minecraft:fall} the
 * same way it names a mob {@code minecraft:zombie}.
 */
final class GameIds {

    private GameIds() {
    }

    /** A damage source's type id, which is a keyed entry of a data-driven registry. */
    static String of(DamageSource source) {
        return source.getDamageType().getKey().toString();
    }

    /** An entity's type id. */
    static String of(Entity entity) {
        return entity.getType().getKey().toString();
    }
}
