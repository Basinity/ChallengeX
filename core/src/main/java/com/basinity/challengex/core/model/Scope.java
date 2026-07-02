package com.basinity.challengex.core.model;

import java.util.Set;

/**
 * Who a building block concerns.
 *
 * <p>Effects accept all three scopes: {@link PerPlayer} hits whoever set off
 * the rule's trigger, {@link EveryPlayer} hits everyone in the run, and
 * {@link SpecificPlayers} hits exactly the players the configurator chose.
 *
 * <p>Triggers and modifiers accept only the {@link Absolute} subset, which
 * names its players outright: a trigger watches everyone or only the chosen
 * players, and a modifier is in force for everyone or only the chosen players.
 * "Per player" is meaningless there (no triggering player exists yet), and the
 * type system rules it out rather than a runtime check.
 */
public sealed interface Scope {

    Scope PER_PLAYER = new PerPlayer();
    Absolute EVERY_PLAYER = new EveryPlayer();

    /** The scopes that name their players outright: every player, or chosen ones. */
    sealed interface Absolute extends Scope {

        default boolean includes(String playerId) {
            return switch (this) {
                case EveryPlayer ignored -> true;
                case SpecificPlayers specific -> specific.playerIds().contains(playerId);
            };
        }
    }

    /** Whoever set off the rule's trigger. Effect-side only. */
    record PerPlayer() implements Scope {
    }

    /** Everyone in the run. */
    record EveryPlayer() implements Absolute {
    }

    /** Explicitly chosen players, one or several. */
    record SpecificPlayers(Set<String> playerIds) implements Absolute {
        public SpecificPlayers {
            playerIds = Set.copyOf(playerIds);
            if (playerIds.isEmpty()) {
                throw new IllegalArgumentException("A specific-player scope requires at least one player");
            }
        }
    }

    static Absolute players(String... playerIds) {
        return new SpecificPlayers(Set.of(playerIds));
    }
}
