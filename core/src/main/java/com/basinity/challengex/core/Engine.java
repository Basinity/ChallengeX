package com.basinity.challengex.core;

import java.util.List;

/**
 * Dispatches abstract game events against the active challenge: an incoming
 * trigger id yields the effect ids of every rule paired to that trigger, in
 * rule order.
 *
 * <p>Platform adapters feed events in and execute the returned effects; the
 * engine itself knows nothing about Minecraft.
 */
public final class Engine {

    private final Challenge challenge;

    public Engine(Challenge challenge) {
        this.challenge = challenge;
    }

    public List<String> onTrigger(String triggerId) {
        return challenge.rules().stream()
                .filter(rule -> rule.triggerId().equals(triggerId))
                .map(Rule::effectId)
                .toList();
    }
}
