package com.basinity.challengex.core.model;

import java.util.Map;

/**
 * A win condition. A challenge carries at most one; completing it ends the run
 * as a win for every player, so goals carry no scope. What completes a goal is
 * declared by its registry definition, not stored here.
 */
public record Goal(String goalId, Map<String, ParamValue> params) {

    public Goal {
        if (goalId == null || goalId.isBlank()) {
            throw new IllegalArgumentException("A goal requires a goal id");
        }
        params = Map.copyOf(params);
    }

    public static Goal of(String goalId) {
        return new Goal(goalId, Map.of());
    }
}
