package com.basinity.challengex.core.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ModelValidationTest {

    @Test
    void ruleRequiresBothTriggerAndEffect() {
        assertThrows(NullPointerException.class,
                () -> new Rule(null, EffectSpec.of("effect.heal")));
        assertThrows(NullPointerException.class,
                () -> new Rule(TriggerSpec.of("trigger.jumped"), null));
        assertThrows(IllegalArgumentException.class, () -> TriggerSpec.of(""));
        assertThrows(IllegalArgumentException.class, () -> EffectSpec.of(""));
    }

    @Test
    void specificPlayerScopeRequiresAtLeastOnePlayer() {
        assertThrows(IllegalArgumentException.class, Scope::players);
    }
}
