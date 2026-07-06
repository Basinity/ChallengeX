package com.basinity.challengex.fabric.trigger;

import net.minecraft.world.damagesource.DamageSource;

/**
 * Turns game objects into the namespaced string ids that trigger parameters
 * match against, so a preset names a damage source {@code minecraft:fall} the
 * same way it names a mob {@code minecraft:zombie}.
 */
final class GameIds {

    private GameIds() {
    }

    /**
     * A damage source's type id. Damage types are a data-driven registry, so the
     * id comes off the holder's key; an inline type carrying no key falls back
     * to its message id, which is what vanilla names it by.
     */
    static String of(DamageSource source) {
        return source.typeHolder().unwrapKey()
                .map(key -> key.identifier().toString())
                .orElseGet(source::getMsgId);
    }
}
