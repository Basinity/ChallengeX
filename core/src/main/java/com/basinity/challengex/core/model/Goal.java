package com.basinity.challengex.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * A win condition. A challenge carries at most one; goals carry no scope. What
 * completes a goal is declared by its registry definition, not stored here.
 * The {@link GoalMode} says how it decides the run (win together, or a versus
 * race), and under win-together the {@link GoalCompletion} says who has to
 * reach it. A versus goal always races individuals, so its completion is
 * normalized to {@link GoalCompletion#ANYONE} and never carries meaning.
 */
public record Goal(String goalId, Map<String, ParamValue> params, GoalMode mode,
        GoalCompletion completion) {

    public Goal {
        if (goalId == null || goalId.isBlank()) {
            throw new IllegalArgumentException("A goal requires a goal id");
        }
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(completion, "completion");
        if (mode == GoalMode.VERSUS) {
            completion = GoalCompletion.ANYONE;
        }
        params = Map.copyOf(params);
    }

    /** The cooperative default: win together, anyone reaching the goal wins for all. */
    public Goal(String goalId, Map<String, ParamValue> params) {
        this(goalId, params, GoalMode.TOGETHER, GoalCompletion.ANYONE);
    }

    public static Goal of(String goalId) {
        return new Goal(goalId, Map.of());
    }
}
