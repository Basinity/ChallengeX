package com.basinity.challengex.core.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A configured effect: which effect, its parameters, and who receives it
 * (whoever triggered, everyone, or explicitly chosen players).
 *
 * <p>The scope is empty exactly when the effect's catalog entry is playerless
 * (it acts on the world or the run rather than on players); validation
 * enforces the match against the registry.
 */
public record EffectSpec(String id, Map<String, ParamValue> params, Optional<Scope> scope) {

    public EffectSpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("An effect requires an effect id");
        }
        params = Map.copyOf(params);
        Objects.requireNonNull(scope, "scope");
    }

    /** A parameterless effect hitting the triggering player, the common case. */
    public static EffectSpec of(String id) {
        return new EffectSpec(id, Map.of(), Optional.of(Scope.PER_PLAYER));
    }

    /** A parameterless effect of a playerless catalog entry, carrying no scope. */
    public static EffectSpec playerless(String id) {
        return new EffectSpec(id, Map.of(), Optional.empty());
    }
}
