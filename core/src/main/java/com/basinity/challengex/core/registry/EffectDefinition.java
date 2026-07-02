package com.basinity.challengex.core.registry;

import java.util.List;

/**
 * An effect catalog entry. Required parameters pick the effect's target
 * (which item, which mob); optional ones tune magnitudes the adapter defaults
 * sensibly. A non-scoped effect acts on the world or the run rather than on
 * players (changing the time, broadcasting a message).
 */
public record EffectDefinition(String id, boolean scoped, List<ParamSpec> params)
        implements Definition {

    public EffectDefinition {
        params = ParamSpec.uniqueNamed(params);
    }
}
