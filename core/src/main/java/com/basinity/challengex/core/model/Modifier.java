package com.basinity.challengex.core.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A persistent, always-on condition for the run: triggerless, unconditional,
 * in force from start to finish. Its scope is {@link Scope.Absolute}: every
 * player, or chosen players (which is what makes asymmetric challenges and
 * per-player handicaps possible).
 *
 * <p>The scope is empty exactly when the modifier's catalog entry is
 * playerless (it applies to the run as a whole, like a time limit);
 * validation enforces the match against the registry.
 */
public record Modifier(String modifierId, Map<String, ParamValue> params, Optional<Scope.Absolute> scope) {

    public Modifier {
        if (modifierId == null || modifierId.isBlank()) {
            throw new IllegalArgumentException("A modifier requires a modifier id");
        }
        params = Map.copyOf(params);
        Objects.requireNonNull(scope, "scope");
    }

    /** A parameterless modifier in force for everyone, the common case. */
    public static Modifier of(String modifierId) {
        return new Modifier(modifierId, Map.of(), Optional.of(Scope.EVERY_PLAYER));
    }
}
