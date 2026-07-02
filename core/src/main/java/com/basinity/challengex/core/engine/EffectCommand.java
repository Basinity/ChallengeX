package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An order to a platform adapter: execute this effect, with these parameters,
 * against these players. The target is already resolved from the effect's
 * scope; {@code AllPlayers} stays symbolic because the engine does not know
 * the server's roster.
 */
public record EffectCommand(String effectId, Map<String, ParamValue> params, Target target) {

    public EffectCommand {
        if (effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("An effect command requires an effect id");
        }
        params = Map.copyOf(params);
        Objects.requireNonNull(target, "target");
    }

    public sealed interface Target {

        Target ALL_PLAYERS = new AllPlayers();

        record AllPlayers() implements Target {
        }

        record Players(Set<String> playerIds) implements Target {
            public Players {
                playerIds = Set.copyOf(playerIds);
                if (playerIds.isEmpty()) {
                    throw new IllegalArgumentException("A player target requires at least one player");
                }
            }
        }

        static Target player(String playerId) {
            return new Players(Set.of(playerId));
        }
    }
}
