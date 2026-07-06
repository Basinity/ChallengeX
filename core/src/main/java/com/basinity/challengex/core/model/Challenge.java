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

    /**
     * The distinct values configured for one parameter of one trigger, across
     * every rule using that trigger. Threshold and schedule triggers (health
     * below a value, every N seconds) describe what to watch for rather than
     * what happened, so their platform sources ask the challenge what the
     * configured rules are watching and detect it themselves. Rules omitting
     * the parameter contribute nothing.
     */
    public List<ParamValue> triggerParamValues(String triggerId, String paramName) {
        return rules.stream()
                .filter(rule -> rule.trigger().id().equals(triggerId))
                .map(rule -> rule.trigger().params().get(paramName))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
