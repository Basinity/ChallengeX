package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.ParamValue;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstract game happening fed in by a platform adapter (or a test). Its id
 * comes from the trigger vocabulary; the context carries what happened (which
 * mob, which block) for trigger-parameter filtering and goal matching. Events
 * without an acting player (weather change, a fixed interval firing) leave
 * {@code playerId} empty.
 */
public record GameEvent(String triggerId, Optional<String> playerId, Map<String, ParamValue> context) {

    public GameEvent {
        if (triggerId == null || triggerId.isBlank()) {
            throw new IllegalArgumentException("An event requires a trigger id");
        }
        Objects.requireNonNull(playerId, "playerId");
        context = Map.copyOf(context);
    }

    public static GameEvent of(String triggerId, String playerId) {
        return new GameEvent(triggerId, Optional.of(playerId), Map.of());
    }

    public static GameEvent of(String triggerId, String playerId, Map<String, ParamValue> context) {
        return new GameEvent(triggerId, Optional.of(playerId), context);
    }

    public static GameEvent playerless(String triggerId) {
        return new GameEvent(triggerId, Optional.empty(), Map.of());
    }

    public static GameEvent playerless(String triggerId, Map<String, ParamValue> context) {
        return new GameEvent(triggerId, Optional.empty(), context);
    }
}
