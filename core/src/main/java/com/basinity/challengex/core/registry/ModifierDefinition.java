package com.basinity.challengex.core.registry;

import java.util.List;

/**
 * A modifier catalog entry: the modifier's id and the parameters it takes.
 * A non-scoped modifier applies to the run as a whole (a time limit) rather
 * than to players.
 */
public record ModifierDefinition(String id, boolean scoped, List<ParamSpec> params)
        implements Definition {

    public ModifierDefinition {
        params = ParamSpec.uniqueNamed(params);
    }
}
