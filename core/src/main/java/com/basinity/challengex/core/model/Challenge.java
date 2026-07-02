package com.basinity.challengex.core.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A challenge aggregates any number of rules, at most one goal, and any number
 * of modifiers. Every piece is independently optional; a modifier-only
 * challenge is a supported shape.
 *
 * <p>Rules stack freely: the same trigger or effect may appear in any number
 * of rules.
 */
public record Challenge(List<Rule> rules, Optional<Goal> goal, List<Modifier> modifiers) {

    public Challenge {
        rules = List.copyOf(rules);
        Objects.requireNonNull(goal, "goal");
        modifiers = List.copyOf(modifiers);
    }

    public static Challenge empty() {
        return new Challenge(List.of(), Optional.empty(), List.of());
    }
}
