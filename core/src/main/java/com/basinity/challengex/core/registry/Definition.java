package com.basinity.challengex.core.registry;

import java.util.List;
import java.util.Optional;

/**
 * A catalog entry: a stable, namespaced id plus the parameters it takes.
 * Triggers, effects, goals, and modifiers all describe themselves this way,
 * which is what lets validation and the web-catalog export treat them
 * uniformly.
 */
public interface Definition {

    String id();

    List<ParamSpec> params();

    /**
     * Whether entries of this definition carry a scope. False means the entry
     * has no player dimension at all: a spec of it must not carry a scope,
     * and where true, a spec of it must. Goals are scopeless in the MVP.
     */
    default boolean scoped() {
        return false;
    }

    default Optional<ParamSpec> param(String name) {
        return params().stream().filter(spec -> spec.name().equals(name)).findFirst();
    }
}
