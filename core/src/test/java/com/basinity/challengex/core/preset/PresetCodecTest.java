package com.basinity.challengex.core.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.Goal;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.CoreCatalog;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class PresetCodecTest {

    private final PresetCodec codec = new PresetCodec(CoreCatalog.createRegistries());

    @Test
    void roundTripPreservesTheChallenge() throws PresetFormatException {
        Preset original = new Preset("Dragon Hunt", new Challenge(
                List.of(new Rule(
                        new TriggerSpec("trigger.mob_killed",
                                Map.of("mob", ParamValue.of("minecraft:zombie")),
                                Optional.of(Scope.players("alice"))),
                        new EffectSpec("effect.apply_status_effect",
                                Map.of("effect", ParamValue.of("minecraft:poison"),
                                        "duration", ParamValue.of(30)),
                                Optional.of(Scope.EVERY_PLAYER)))),
                Optional.of(new Goal("goal.kill_mob",
                        Map.of("mob", ParamValue.of("minecraft:ender_dragon")))),
                List.of(
                        new Modifier("modifier.disable_jump",
                                Map.of(),
                                Optional.of(Scope.players("bob")), OptionalLong.of(6000)),
                        new Modifier("modifier.time_limit",
                                Map.of("minutes", ParamValue.of(30)),
                                Optional.empty(), OptionalLong.empty()))));

        assertEquals(original, codec.fromJson(codec.toJson(original)));
    }

    @Test
    void playerlessEntriesRoundTripWithoutScopeFields() throws PresetFormatException {
        Preset original = new Preset("Nightfall", new Challenge(
                List.of(new Rule(
                        TriggerSpec.playerless("trigger.weather_changed"),
                        new EffectSpec("effect.change_time",
                                Map.of("value", ParamValue.of("night")), Optional.empty()))),
                Optional.empty(),
                List.of()));

        String json = codec.toJson(original);

        assertFalse(json.contains("\"scope\""));
        assertEquals(original, codec.fromJson(json));
    }

    @Test
    void unknownIdsAreAllNamedAtOnce() {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "Broken",
                  "rules": [{"trigger": {"id": "trigger.bogus"}, "effect": {"id": "effect.heal"}}],
                  "modifiers": [{"id": "modifier.fake"}]
                }""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("trigger.bogus"));
        assertTrue(rejection.getMessage().contains("modifier.fake"));
        assertTrue(rejection.getMessage().contains("newer ChallengeX"));
    }

    @Test
    void newerSchemaVersionIsRejectedWithAnUpdatePointer() {
        String json = """
                {"schemaVersion": 999, "name": "From the future"}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("schema version 999"));
        assertTrue(rejection.getMessage().contains("update ChallengeX"));
    }

    @Test
    void missingRequiredParameterIsRejected() {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "No mob picked",
                  "rules": [{"trigger": {"id": "trigger.jumped"}, "effect": {"id": "effect.spawn_mob"}}]
                }""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("missing required parameter 'mob'"));
    }

    @Test
    void wronglyTypedParameterIsRejected() {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "Typed wrong",
                  "rules": [{"trigger": {"id": "trigger.jumped"},
                             "effect": {"id": "effect.apply_status_effect",
                                        "params": {"effect": "minecraft:poison", "duration": "long"}}}]
                }""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("'duration'"));
        assertTrue(rejection.getMessage().contains("INT"));
    }

    @Test
    void undeclaredParameterIsRejected() {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "Extra param",
                  "rules": [{"trigger": {"id": "trigger.jumped"},
                             "effect": {"id": "effect.heal", "params": {"strength": 5}}}]
                }""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("unknown parameter 'strength'"));
    }

    @Test
    void missingScopeIsRejectedOnScopedEntries() {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "Unscoped",
                  "rules": [{"trigger": {"id": "trigger.jumped"}, "effect": {"id": "effect.heal"}}],
                  "modifiers": [{"id": "modifier.keep_inventory"}]
                }""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("missing scope for 'trigger.jumped'"));
        assertTrue(rejection.getMessage().contains("missing scope for 'effect.heal'"));
        assertTrue(rejection.getMessage().contains("missing scope for 'modifier.keep_inventory'"));
    }

    @Test
    void scopeOnPlayerlessEntriesIsRejected() {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "Overscoped",
                  "rules": [{"trigger": {"id": "trigger.weather_changed", "scope": "every_player"},
                             "effect": {"id": "effect.change_time",
                                        "params": {"value": "day"}, "scope": "every_player"}}],
                  "modifiers": [{"id": "modifier.buff_hostile_mobs", "scope": "every_player"}]
                }""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("'trigger.weather_changed' has no player dimension"));
        assertTrue(rejection.getMessage().contains("'effect.change_time' has no player dimension"));
        assertTrue(rejection.getMessage().contains("'modifier.buff_hostile_mobs' has no player dimension"));
    }

    @Test
    void perPlayerScopeIsInvalidOnATrigger() {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "Scoped wrong",
                  "rules": [{"trigger": {"id": "trigger.jumped", "scope": "per_player"},
                             "effect": {"id": "effect.heal"}}]
                }""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("only valid on effects"));
    }

    @Test
    void integralNumbersAreAcceptedForDecimalParameters() throws PresetFormatException {
        String json = """
                {
                  "schemaVersion": 1,
                  "name": "Whole hearts",
                  "rules": [{"trigger": {"id": "trigger.jumped", "scope": "every_player"},
                             "effect": {"id": "effect.damage", "params": {"hearts": 3},
                                        "scope": "per_player"}}]
                }""";

        Preset preset = codec.fromJson(json);

        assertEquals(ParamValue.of(3L),
                preset.challenge().rules().getFirst().effect().params().get("hearts"));
    }

    @Test
    void malformedJsonIsRejected() {
        assertThrows(PresetFormatException.class, () -> codec.fromJson("not json {{"));
    }
}
