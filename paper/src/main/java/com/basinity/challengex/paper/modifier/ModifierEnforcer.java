package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.core.model.Modifier;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Applies one catalog modifier's continuous effect to a player. One enforcer
 * per modifier id; {@link ModifierEnforcers} maps ids to enforcers and
 * {@link ModifierEnforcementTickSource} dispatches to them once a tick.
 *
 * <p>{@link #start} and {@link #stop} fire exactly once each, on the tick a
 * modifier becomes active or stops being active for a player, for state that
 * needs applying once and reverting cleanly (an attribute modifier). {@link
 * #tick} fires every tick the modifier stays active, for state that decays or
 * can be bypassed and needs continuous reapplication (a status effect).
 */
public interface ModifierEnforcer {

    default void start(Player player, Modifier modifier, Server server) {
    }

    default void tick(Player player, Modifier modifier, Server server) {
    }

    default void stop(Player player, Modifier modifier, Server server) {
    }

    /**
     * Clears any cross-player state the enforcer holds outside per-player
     * lifecycle, on plugin disable. Most enforcers keep no such state; a
     * shared-across-players enforcer uses it so a reload starts from a clean
     * slate rather than inheriting the previous one's.
     */
    default void disabled() {
    }
}
