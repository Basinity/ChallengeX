package com.basinity.challengex.core.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.engine.Completion;
import com.basinity.challengex.core.engine.RunOutcome;
import com.basinity.challengex.core.engine.RunSnapshot;
import com.basinity.challengex.core.engine.RunState;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.CoreCatalog;
import java.util.List;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RunSnapshotCodecTest {

    private final RunSnapshotCodec codec = new RunSnapshotCodec(CoreCatalog.createRegistries());

    private static Challenge sampleChallenge() {
        return new Challenge(
                List.of(new Rule(
                        new TriggerSpec("trigger.mob_killed",
                                Map.of("mob", ParamValue.of("minecraft:zombie")),
                                Optional.of(Scope.EVERY_PLAYER)),
                        new EffectSpec("effect.apply_status_effect",
                                Map.of("effect", ParamValue.of("minecraft:poison"),
                                        "duration", ParamValue.of(30)),
                                Optional.of(Scope.PER_PLAYER)))),
                List.of(new Modifier("modifier.time_limit",
                        Map.of("minutes", ParamValue.of(30)),
                        Optional.empty())));
    }

    @Test
    void roundTripPreservesEveryField() throws PresetFormatException {
        RunSnapshot original = new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, sampleChallenge(),
                RunState.RUNNING, 4321L, RunOutcome.ONGOING,
                List.of(new Completion("Pix", 900L)), Set.of("Kettu"));

        assertEquals(original, codec.fromJson(codec.toJson(original)));
    }

    @Test
    void pausedAndFinishedStatesRoundTrip() throws PresetFormatException {
        RunSnapshot paused = new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, sampleChallenge(),
                RunState.PAUSED, 100L, RunOutcome.ONGOING, List.of(), Set.of());
        RunSnapshot finished = new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, sampleChallenge(),
                RunState.FINISHED, 6000L, RunOutcome.WIN,
                List.of(new Completion("Basinity", 1200L)), Set.of());

        assertEquals(paused, codec.fromJson(codec.toJson(paused)));
        assertEquals(finished, codec.fromJson(codec.toJson(finished)));
    }

    @Test
    void finishingOrderSurvivesTheRoundTripRatherThanBeingSorted() throws PresetFormatException {
        // Third alphabetically, first across the line: the order written is the
        // placing, so sorting anywhere in the codec would rewrite the results.
        RunSnapshot original = new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, sampleChallenge(),
                RunState.RUNNING, 5000L, RunOutcome.ONGOING,
                List.of(new Completion("Zoe", 400L),
                        new Completion("Basinity", 900L),
                        new Completion("Pix", 4000L)),
                Set.of());

        RunSnapshot back = codec.fromJson(codec.toJson(original));

        assertEquals(List.of("Zoe", "Basinity", "Pix"),
                back.completions().stream().map(Completion::playerId).toList());
        assertEquals(List.of(400L, 900L, 4000L),
                back.completions().stream().map(Completion::atTick).toList());
    }

    @Test
    void anOlderSnapshotIsRefusedRatherThanResumedWithItsOutcomesMissing() {
        String json = """
                {"snapshotVersion": 1, "state": "RUNNING", "elapsedTicks": 10,
                 "outcome": "ONGOING", "goalProgress": [0], "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("snapshot version 1"), rejection.getMessage());
        assertTrue(rejection.getMessage().contains("cannot be resumed"), rejection.getMessage());
    }

    @Test
    void aPlayerFinishingTwiceIsRejected() {
        String json = """
                {"snapshotVersion": 2, "state": "RUNNING", "elapsedTicks": 10,
                 "outcome": "ONGOING",
                 "completions": [{"player": "Pix", "atTick": 10},
                                 {"player": "Pix", "atTick": 20}],
                 "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("finishes twice"), rejection.getMessage());
    }

    @Test
    void aMalformedCompletionIsRejected() {
        String json = """
                {"snapshotVersion": 2, "state": "RUNNING", "elapsedTicks": 10,
                 "outcome": "ONGOING",
                 "completions": [{"player": "", "atTick": -5}],
                 "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("non-blank 'player'"), rejection.getMessage());
        assertTrue(rejection.getMessage().contains("non-negative whole number"), rejection.getMessage());
    }

    @Test
    void newerSnapshotVersionIsRejectedWithAnUpdatePointer() {
        String json = """
                {"snapshotVersion": 999, "state": "RUNNING", "elapsedTicks": 0,
                 "outcome": "ONGOING", "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("snapshot version 999"));
        assertTrue(rejection.getMessage().contains("update ChallengeX"));
    }

    @Test
    void unknownStateIsRejected() {
        String json = """
                {"snapshotVersion": 2, "state": "SPINNING", "elapsedTicks": 0,
                 "outcome": "ONGOING", "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("unknown state 'SPINNING'"));
    }

    @Test
    void negativeElapsedTicksIsRejected() {
        String json = """
                {"snapshotVersion": 2, "state": "RUNNING", "elapsedTicks": -5,
                 "outcome": "ONGOING", "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("elapsedTicks"));
    }

    @Test
    void aMalformedChallengeIsRejectedThroughTheSharedValidation() {
        String json = """
                {"snapshotVersion": 2, "state": "RUNNING", "elapsedTicks": 0,
                 "outcome": "ONGOING",
                 "challenge": {"rules": [{"trigger": {"id": "trigger.bogus"},
                                          "effect": {"id": "effect.heal"}}]}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("trigger.bogus"));
    }

    @Test
    void malformedJsonIsRejected() {
        assertThrows(PresetFormatException.class, () -> codec.fromJson("not json {{"));
    }
}
