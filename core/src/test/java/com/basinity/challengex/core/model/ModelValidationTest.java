package com.basinity.challengex.core.model;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class ModelValidationTest {

    @Test
    void ruleRequiresBothTriggerAndEffect() {
        assertThrows(NullPointerException.class,
                () -> new Rule(null, EffectSpec.of("effect.heal")));
        assertThrows(NullPointerException.class,
                () -> new Rule(TriggerSpec.of("trigger.jump"), null));
        assertThrows(IllegalArgumentException.class, () -> TriggerSpec.of(""));
        assertThrows(IllegalArgumentException.class, () -> EffectSpec.of(""));
    }

    @Test
    void specificPlayerScopeRequiresAtLeastOnePlayer() {
        assertThrows(IllegalArgumentException.class, Scope::players);
    }

    @Test
    void modifierExpiryMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Modifier(
                "modifier.keep_inventory", Map.of(), Optional.of(Scope.EVERY_PLAYER),
                OptionalLong.of(0)));
    }
}
