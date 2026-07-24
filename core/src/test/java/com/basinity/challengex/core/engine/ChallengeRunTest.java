package com.basinity.challengex.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Registries;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ChallengeRunTest {

    private final Registries registries = CoreCatalog.createRegistries();

    private ChallengeRun runFor(EffectExecutor executor, Rule... rules) {
        return new ChallengeRun(
                new Challenge(List.of(rules), List.of()), registries, executor);
    }

    private ChallengeRun runOf(Challenge challenge, EffectExecutor executor) {
        return new ChallengeRun(challenge, registries, executor);
    }

    @Test
    void mobKilledFiresStatusEffectOnTheKiller() {
        List<EffectCommand> executed = new ArrayList<>();
        ChallengeRun run = runFor(executed::add, new Rule(
                TriggerSpec.of("trigger.mob_killed"),
                new EffectSpec("effect.apply_status_effect",
                        Map.of("effect", ParamValue.of("minecraft:poison"),
                                "duration", ParamValue.of(200L)),
                        Optional.of(Scope.PER_PLAYER))));
        run.start();

        run.handle(GameEvent.of("trigger.mob_killed", "alice",
                Map.of("mob", ParamValue.of("minecraft:zombie"))));

        assertEquals(1, executed.size());
        EffectCommand command = executed.getFirst();
        assertEquals("effect.apply_status_effect", command.effectId());
        assertEquals(ParamValue.of("minecraft:poison"), command.params().get("effect"));
        assertEquals(EffectCommand.Target.player("alice"), command.target());
    }

    @Test
    void loseChallengeNeverReachesTheExecutor() {
        List<EffectCommand> executed = new ArrayList<>();
        ChallengeRun run = runFor(executed::add, new Rule(
                TriggerSpec.of("trigger.player_died"),
                new EffectSpec(CoreCatalog.EFFECT_LOSE_CHALLENGE, Map.of(),
                        Optional.of(Scope.PER_PLAYER))));
        run.start();
        run.updateRoster(List.of("alice"));

        run.handle(GameEvent.of("trigger.player_died", "alice"));
        run.updateRoster(List.of());

        assertTrue(executed.isEmpty());
        assertEquals(RunOutcome.LOSS, run.outcome());
        assertEquals(RunState.FINISHED, run.state());
    }

    @Test
    void aFreshRunWaitsToStart() {
        List<EffectCommand> executed = new ArrayList<>();
        ChallengeRun run = runFor(executed::add, Rule.of("trigger.jumped", "effect.heal"));

        assertEquals(RunState.NOT_STARTED, run.state());
        run.handle(GameEvent.of("trigger.jumped", "alice"));

        assertTrue(executed.isEmpty(), "events must not fire before the run starts");
    }

    @Test
    void pauseFreezesDispatchAndTheClockUntilResumed() {
        List<EffectCommand> executed = new ArrayList<>();
        ChallengeRun run = runFor(executed::add, Rule.of("trigger.jumped", "effect.heal"));
        run.start();

        run.tick(40);
        run.pause();
        run.tick(1000);
        run.handle(GameEvent.of("trigger.jumped", "alice"));

        assertEquals(40, run.elapsedTicks(), "clock must not advance while paused");
        assertTrue(executed.isEmpty(), "events must not fire while paused");

        run.resume();
        run.handle(GameEvent.of("trigger.jumped", "alice"));
        assertEquals(1, executed.size());
    }

    @Test
    void resetReturnsToAFreshNotStartedRun() {
        List<EffectCommand> executed = new ArrayList<>();
        ChallengeRun run = runFor(executed::add, Rule.of("trigger.jumped", "effect.heal"));
        run.start();
        run.tick(200);

        run.reset();

        assertEquals(RunState.NOT_STARTED, run.state());
        assertEquals(0, run.elapsedTicks());
        run.handle(GameEvent.of("trigger.jumped", "alice"));
        assertTrue(executed.isEmpty(), "a reset run must wait to be started again");
    }

    @Test
    void modifiersAreInForceOnlyWhileTheRunIsLive() {
        Challenge challenge = new Challenge(List.of(),
                List.of(Modifier.of("modifier.keep_inventory")));
        ChallengeRun run = runOf(challenge, command -> { });

        assertTrue(run.activeModifiersFor("alice").isEmpty(), "nothing enforced before start");
        run.start();
        assertEquals(1, run.activeModifiersFor("alice").size());
        run.pause();
        assertEquals(1, run.activeModifiersFor("alice").size(), "still in force, just frozen");
    }

    @Test
    void aTimeLimitCountsDownAndEndsTheRunAsALoss() {
        Challenge challenge = new Challenge(List.of(),
                List.of(new Modifier("modifier.time_limit", Map.of("minutes", ParamValue.of(1)),
                        Optional.empty())));
        ChallengeRun run = runOf(challenge, command -> { });
        run.start();

        assertEquals(1200, run.displayTicks(), "one minute is 1200 ticks, counting down");
        run.tick(800);
        assertEquals(400, run.displayTicks());
        assertEquals(RunState.RUNNING, run.state());

        run.tick(400);
        assertEquals(0, run.displayTicks());
        assertEquals(RunOutcome.LOSS, run.outcome());
        assertEquals(RunState.FINISHED, run.state());
        assertTrue(run.activeModifiersFor("alice").isEmpty(), "a finished run enforces nothing");
    }

    @Test
    void withoutATimeLimitTheClockCountsUp() {
        ChallengeRun run = runFor(command -> { }, Rule.of("trigger.jumped", "effect.heal"));
        run.start();

        run.tick(60);
        assertEquals(60, run.displayTicks());
        assertTrue(run.timeLimitTicks().isEmpty());
    }

    @Test
    void snapshotAndRestoreResumeARunMidClock() {
        List<EffectCommand> executed = new ArrayList<>();
        ChallengeRun run = runFor(executed::add, Rule.of("trigger.jumped", "effect.heal"));
        run.start();
        run.pause();

        RunSnapshot snapshot = run.snapshot();
        assertEquals(RunState.PAUSED, snapshot.state());

        ChallengeRun restored = ChallengeRun.restore(snapshot, registries, executed::add);
        assertEquals(RunState.PAUSED, restored.state());

        // A paused restored run stays frozen until resumed, then keeps its clock.
        restored.resume();
        restored.tick(75);
        assertEquals(75, restored.elapsedTicks());
        restored.handle(GameEvent.of("trigger.jumped", "alice"));
        assertEquals(1, executed.size());
    }

    @Test
    void restoringAFinishedRunKeepsItsOutcome() {
        Challenge challenge = new Challenge(List.of(),
                List.of(new Modifier("modifier.time_limit", Map.of("minutes", ParamValue.of(1)),
                        Optional.empty())));
        ChallengeRun run = runOf(challenge, command -> { });
        run.start();
        run.tick(1200);
        assertEquals(RunState.FINISHED, run.state());

        ChallengeRun restored = ChallengeRun.restore(run.snapshot(), registries, command -> { });

        assertEquals(RunState.FINISHED, restored.state());
        assertEquals(RunOutcome.LOSS, restored.outcome());
        assertEquals(1200, restored.elapsedTicks());
    }

    /**
     * The point of persisting outcomes: a run interrupted partway through a race
     * comes back with the same finishing order and the same casualties, rather
     * than resuming as though nobody had done anything yet.
     */
    @Test
    void restoringARunMidRaceKeepsTheFinishingOrderAndWhoIsOut() {
        ChallengeRun run = runFor(command -> { },
                new Rule(TriggerSpec.of("trigger.game_beaten"),
                        new EffectSpec(CoreCatalog.EFFECT_WIN_CHALLENGE,
                                Map.of("end", ParamValue.of(CoreCatalog.END_AFTER_ALL_COMPLETE)),
                                Optional.of(Scope.PER_PLAYER))),
                new Rule(TriggerSpec.of("trigger.player_died"),
                        new EffectSpec(CoreCatalog.EFFECT_LOSE_CHALLENGE, Map.of(),
                                Optional.of(Scope.PER_PLAYER))));
        run.start();
        run.updateRoster(List.of("Pix", "Kettu", "Basinity"));

        run.tick(400);
        run.handle(GameEvent.of("trigger.game_beaten", "Kettu"));
        run.tick(500);
        run.handle(GameEvent.of("trigger.game_beaten", "Pix"));
        run.handle(GameEvent.of("trigger.player_died", "Basinity"));

        ChallengeRun restored = ChallengeRun.restore(run.snapshot(), registries, command -> { });

        assertEquals(RunState.RUNNING, restored.state());
        assertEquals(RunOutcome.ONGOING, restored.outcome());
        assertEquals(List.of("Kettu", "Pix"),
                restored.completions().stream().map(Completion::playerId).toList());
        assertEquals(List.of(400L, 900L),
                restored.completions().stream().map(Completion::atTick).toList());
        assertEquals(Set.of("Basinity"), restored.eliminated());

        // And the restored run still finishes the way the original would have:
        // the roster empties, and somebody finished, so it is a win.
        restored.updateRoster(List.of());
        assertEquals(RunOutcome.WIN, restored.outcome());
    }
}
