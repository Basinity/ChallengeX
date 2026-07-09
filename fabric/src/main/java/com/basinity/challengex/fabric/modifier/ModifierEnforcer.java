package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Applies one catalog modifier's continuous effect to a player. One enforcer
 * per modifier id; {@link ModifierEnforcers} maps ids to enforcers and {@code
 * ModifierEnforcementTickSource} dispatches to them once a server tick.
 *
 * <p>{@link #start} and {@link #stop} fire exactly once each, on the tick a
 * modifier becomes active or stops being active for a player, for state that
 * needs applying once and reverting cleanly (an attribute modifier). {@link
 * #tick} fires every tick the modifier stays active, for state that decays or
 * can be bypassed and needs continuous reapplication (a status effect).
 */
public interface ModifierEnforcer {

    default void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
    }

    default void tick(ServerPlayer player, Modifier modifier, MinecraftServer server) {
    }

    default void stop(ServerPlayer player, Modifier modifier, MinecraftServer server) {
    }

    /**
     * Clears any cross-player state the enforcer holds outside per-player
     * lifecycle, on server stop. Most enforcers keep no such state and need not
     * override this; a shared-across-players enforcer uses it so a fresh world
     * starts from a clean slate rather than inheriting the previous world's.
     */
    default void serverStopped() {
    }
}
