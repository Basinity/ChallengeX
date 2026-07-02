package com.basinity.challengex.core.model;

import java.util.Objects;

/**
 * A rule pairs a trigger with an effect. Both are required: a rule without a
 * trigger or without an effect is not a supported shape. Each half is a
 * self-contained block carrying its own parameters and scope.
 */
public record Rule(TriggerSpec trigger, EffectSpec effect) {

    public Rule {
        Objects.requireNonNull(trigger, "A rule requires a trigger");
        Objects.requireNonNull(effect, "A rule requires an effect");
    }

    /** A parameterless, default-scoped rule, the common case. */
    public static Rule of(String triggerId, String effectId) {
        return new Rule(TriggerSpec.of(triggerId), EffectSpec.of(effectId));
    }
}
