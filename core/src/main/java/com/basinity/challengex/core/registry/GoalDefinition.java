package com.basinity.challengex.core.registry;

import java.util.List;

/**
 * A goal catalog entry: parameters plus the requirements whose joint
 * fulfillment completes the goal. Declaring completion as data keeps the
 * engine generic and let's compound goals reuse the same evaluation.
 */
public record GoalDefinition(String id, List<GoalRequirement> requirements, List<ParamSpec> params) implements Definition {

    public GoalDefinition {
        params = ParamSpec.uniqueNamed(params);
        requirements = List.copyOf(requirements);
        if (requirements.isEmpty()) {
            throw new IllegalArgumentException("A goal definition needs at least one requirement");
        }
    }
}
