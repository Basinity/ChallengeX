package com.basinity.challengex.core;

import java.util.List;

/**
 * A challenge aggregates any number of rules. Goals and modifiers join this
 * aggregate in the core engine phase.
 *
 * <p>Rules form a plain multiset: the same trigger or effect may appear in any
 * number of rules, and duplicates are allowed.
 */
public record Challenge(List<Rule> rules) {

    public Challenge {
        rules = List.copyOf(rules);
    }

    public static Challenge empty() {
        return new Challenge(List.of());
    }
}
