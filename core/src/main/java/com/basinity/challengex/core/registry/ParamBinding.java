package com.basinity.challengex.core.registry;

import com.basinity.challengex.core.model.ParamValue;
import java.util.Objects;

/**
 * Where a goal requirement gets the value an event's context must match:
 * a fixed literal, or one of the goal's own parameters.
 */
public sealed interface ParamBinding {

    record Literal(ParamValue value) implements ParamBinding {
        public Literal {
            Objects.requireNonNull(value, "value");
        }
    }

    record FromGoalParam(String goalParamName) implements ParamBinding {
        public FromGoalParam {
            if (goalParamName == null || goalParamName.isBlank()) {
                throw new IllegalArgumentException("A goal-parameter binding requires a parameter name");
            }
        }
    }

    static ParamBinding literal(String value) {
        return new Literal(ParamValue.of(value));
    }

    static ParamBinding fromGoalParam(String goalParamName) {
        return new FromGoalParam(goalParamName);
    }
}
