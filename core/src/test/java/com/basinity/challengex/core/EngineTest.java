package com.basinity.challengex.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class EngineTest {

    @Test
    void matchingTriggerDispatchesPairedEffect() {
        Challenge challenge = new Challenge(List.of(
                new Rule("trigger.damage_taken", "effect.random_negative_effect")));
        Engine engine = new Engine(challenge);

        assertEquals(List.of("effect.random_negative_effect"),
                engine.onTrigger("trigger.damage_taken"));
    }

    @Test
    void nonMatchingTriggerDispatchesNothing() {
        Challenge challenge = new Challenge(List.of(
                new Rule("trigger.damage_taken", "effect.random_negative_effect")));
        Engine engine = new Engine(challenge);

        assertEquals(List.of(), engine.onTrigger("trigger.block_broken"));
    }

    @Test
    void rulesStackFreelyOnTheSameTrigger() {
        Challenge challenge = new Challenge(List.of(
                new Rule("trigger.jump", "effect.damage_player"),
                new Rule("trigger.jump", "effect.drain_hunger")));
        Engine engine = new Engine(challenge);

        assertEquals(List.of("effect.damage_player", "effect.drain_hunger"),
                engine.onTrigger("trigger.jump"));
    }

    @Test
    void ruleRequiresBothTriggerAndEffect() {
        assertThrows(IllegalArgumentException.class,
                () -> new Rule("trigger.damage_taken", ""));
        assertThrows(IllegalArgumentException.class,
                () -> new Rule("", "effect.random_negative_effect"));
    }
}
