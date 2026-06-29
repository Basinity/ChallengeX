package com.basinity.challengex.core;

/**
 * A rule pairs a trigger with an effect. Both are required: a rule without a
 * trigger or without an effect is not a supported shape.
 *
 * <p>Walking skeleton: ids only. Parameters and scope follow in the core
 * engine phase.
 */
public record Rule(String triggerId, String effectId) {

    public Rule {
        if (triggerId == null || triggerId.isBlank()) {
            throw new IllegalArgumentException("A rule requires a trigger id");
        }
        if (effectId == null || effectId.isBlank()) {
            throw new IllegalArgumentException("A rule requires an effect id");
        }
    }
}
