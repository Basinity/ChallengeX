package com.basinity.challengex.core.registry;

import java.util.List;

/**
 * A trigger catalog entry. Its parameters act as filters when configured in a
 * rule, except the ones marked required, without which the trigger is
 * meaningless (an interval trigger needs its interval). A non-scoped trigger
 * fires without an acting player (a weather change, an interval elapsing).
 */
public record TriggerDefinition(String id, boolean scoped, List<ParamSpec> params)
        implements Definition {

    public TriggerDefinition {
        params = ParamSpec.uniqueNamed(params);
    }
}
