package com.basinity.challengex.core.registry;

import java.util.Map;
import java.util.Set;

/**
 * One condition a goal needs met: an event whose id is any of
 * {@code eventIds} (the trigger-id vocabulary doubles as the event
 * vocabulary) and whose context matches every binding. A goal completes when
 * all of its requirements have been met at least once; a single-requirement
 * goal completes immediately, a compound goal (defeat all bosses) accumulates.
 */
public record GoalRequirement(Set<String> eventIds, Map<String, ParamBinding> contextMatch) {

    public GoalRequirement {
        eventIds = Set.copyOf(eventIds);
        if (eventIds.isEmpty()) {
            throw new IllegalArgumentException("A goal requirement needs at least one event id");
        }
        contextMatch = Map.copyOf(contextMatch);
    }
}
