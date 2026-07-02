package com.basinity.challengex.core.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A configured trigger: which trigger, its parameters, and whose activity it
 * watches. Parameters act as filters (a block-broken trigger configured with
 * a block only fires for that block); an omitted parameter matches anything.
 *
 * <p>The scope is empty exactly when the trigger's catalog entry is playerless
 * (it fires without an acting player); validation enforces the match against
 * the registry.
 */
public record TriggerSpec(String id, Map<String, ParamValue> params, Optional<Scope.Absolute> scope) {

    public TriggerSpec {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("A trigger requires a trigger id");
        }
        params = Map.copyOf(params);
        Objects.requireNonNull(scope, "scope");
    }

    /** A parameterless trigger watching every player, the common case. */
    public static TriggerSpec of(String id) {
        return new TriggerSpec(id, Map.of(), Optional.of(Scope.EVERY_PLAYER));
    }

    /** A parameterless trigger of a playerless catalog entry, carrying no scope. */
    public static TriggerSpec playerless(String id) {
        return new TriggerSpec(id, Map.of(), Optional.empty());
    }
}
