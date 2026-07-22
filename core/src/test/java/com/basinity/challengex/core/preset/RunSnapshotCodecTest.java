package com.basinity.challengex.core.preset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.engine.RunOutcome;
import com.basinity.challengex.core.engine.RunSnapshot;
import com.basinity.challengex.core.engine.RunState;
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
                Optional.of(new Goal("goal.kill_mob",
                        Map.of("mob", ParamValue.of("minecraft:ender_dragon")))),
                List.of(new Modifier("modifier.time_limit",
                        Map.of("minutes", ParamValue.of(30)),
                        Optional.empty())));
    }

    @Test
    void roundTripPreservesEveryField() throws PresetFormatException {
        RunSnapshot original = new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, sampleChallenge(),
                RunState.RUNNING, 4321L, RunOutcome.ONGOING, Set.of(0, 2),
                Map.of("Basinity", Set.of(0), "Pix", Set.of(0, 2)), Optional.empty());

        assertEquals(original, codec.fromJson(codec.toJson(original)));
    }

    @Test
    void pausedAndFinishedStatesRoundTrip() throws PresetFormatException {
        RunSnapshot paused = new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, sampleChallenge(),
                RunState.PAUSED, 100L, RunOutcome.ONGOING, Set.of(), Map.of(), Optional.empty());
        RunSnapshot finished = new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, sampleChallenge(),
                RunState.FINISHED, 6000L, RunOutcome.WIN, Set.of(0),
                Map.of("Basinity", Set.of(0)), Optional.of("Basinity"));

        assertEquals(paused, codec.fromJson(codec.toJson(paused)));
        assertEquals(finished, codec.fromJson(codec.toJson(finished)));
    }

    @Test
    void aSnapshotWithoutTheNewGoalFieldsStillReads() throws PresetFormatException {
        // Written by a build predating per-player goal progress and the winner.
        String json = """
                {"snapshotVersion": 1, "state": "RUNNING", "elapsedTicks": 10,
                 "outcome": "ONGOING", "goalProgress": [0],
                 "challenge": {"goal": {"id": "goal.kill_mob",
                                        "params": {"mob": "minecraft:ender_dragon"}}}}""";

        RunSnapshot snapshot = codec.fromJson(json);

        assertEquals(Map.of(), snapshot.goalProgressByPlayer());
        assertEquals(Optional.empty(), snapshot.winner());
    }

    @Test
    void newerSnapshotVersionIsRejectedWithAnUpdatePointer() {
        String json = """
                {"snapshotVersion": 999, "state": "RUNNING", "elapsedTicks": 0,
                 "outcome": "ONGOING", "goalProgress": [], "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("snapshot version 999"));
        assertTrue(rejection.getMessage().contains("update ChallengeX"));
    }

    @Test
    void unknownStateIsRejected() {
        String json = """
                {"snapshotVersion": 1, "state": "SPINNING", "elapsedTicks": 0,
                 "outcome": "ONGOING", "goalProgress": [], "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("unknown state 'SPINNING'"));
    }

    @Test
    void negativeElapsedTicksIsRejected() {
        String json = """
                {"snapshotVersion": 1, "state": "RUNNING", "elapsedTicks": -5,
                 "outcome": "ONGOING", "goalProgress": [], "challenge": {}}""";

        PresetFormatException rejection =
                assertThrows(PresetFormatException.class, () -> codec.fromJson(json));

        assertTrue(rejection.getMessage().contains("elapsedTicks"));
    }

    @Test
    void aMalformedChallengeIsRejectedThroughTheSharedValidation() {
        String json = """
                {"snapshotVersion": 1, "state": "RUNNING", "elapsedTicks": 0,
                 "outcome": "ONGOING", "goalProgress": [],
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
