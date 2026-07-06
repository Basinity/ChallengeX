package com.basinity.challengex.core.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The query threshold and schedule trigger sources ask what to watch for. They
 * echo the values it returns straight back as event context, so what comes out
 * has to be exactly what the preset configured.
 */
class TriggerParamValuesTest {

    private static Rule watching(String triggerId, String paramName, ParamValue value) {
        return new Rule(
                new TriggerSpec(triggerId, Map.of(paramName, value), Optional.of(Scope.EVERY_PLAYER)),
                EffectSpec.of("effect.kill"));
    }

    @Test
    void collectsEveryConfiguredThresholdForATrigger() {
        Challenge challenge = new Challenge(List.of(
                watching("trigger.health_below", "hearts", ParamValue.of(5L)),
                watching("trigger.health_below", "hearts", ParamValue.of(2L))),
                Optional.empty(), List.of());

        List<ParamValue> thresholds = challenge.triggerParamValues("trigger.health_below", "hearts");

        assertEquals(2, thresholds.size());
        assertTrue(thresholds.contains(ParamValue.of(5L)));
        assertTrue(thresholds.contains(ParamValue.of(2L)));
    }

    @Test
    void repeatsNoValueTwiceHoweverManyRulesShareIt() {
        Challenge challenge = new Challenge(List.of(
                watching("trigger.fixed_interval", "seconds", ParamValue.of(300L)),
                watching("trigger.fixed_interval", "seconds", ParamValue.of(300L))),
                Optional.empty(), List.of());

        assertEquals(List.of(ParamValue.of(300L)),
                challenge.triggerParamValues("trigger.fixed_interval", "seconds"));
    }

    @Test
    void ignoresOtherTriggersAndOtherParameters() {
        Challenge challenge = new Challenge(List.of(
                watching("trigger.health_below", "hearts", ParamValue.of(5L)),
                watching("trigger.hunger_below", "points", ParamValue.of(6L))),
                Optional.empty(), List.of());

        assertEquals(List.of(ParamValue.of(6L)),
                challenge.triggerParamValues("trigger.hunger_below", "points"));
        assertTrue(challenge.triggerParamValues("trigger.health_below", "points").isEmpty());
        assertTrue(challenge.triggerParamValues("trigger.jumped", "hearts").isEmpty());
    }

    @Test
    void keepsTheConfiguredValuesOwnShapeSoTheEngineMatchesItBack() {
        // A preset writing 5 rather than 5.0 must come back as the int it was:
        // the engine matches context by equality, and OfInt(5) is not OfDecimal(5.0).
        Challenge challenge = new Challenge(
                List.of(watching("trigger.health_below", "hearts", ParamValue.of(5L))),
                Optional.empty(), List.of());

        ParamValue threshold = challenge.triggerParamValues("trigger.health_below", "hearts").getFirst();

        assertEquals(ParamValue.of(5L), threshold);
        assertTrue(threshold instanceof ParamValue.OfInt);
    }
}
