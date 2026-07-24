package com.basinity.challengex.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The per-player outcome model: who wins, who is out, and when the run ends.
 *
 * <p>The four shapes come from two independent choices. The win effect's scope
 * decides who is awarded the win (every player wins together, per player keeps
 * it to whoever finished), and its {@code end} parameter decides when the run
 * stops (at the first completion, or once nobody is left playing). A completion
 * is always credited to the player who triggered it whatever the scope.
 */
class OutcomeModelTest {

    private final Registries registries = CoreCatalog.createRegistries();

    // ---- builders ----

    private static Rule winRule(String triggerId, Scope scope, String end) {
        return new Rule(TriggerSpec.of(triggerId),
                new EffectSpec(CoreCatalog.EFFECT_WIN_CHALLENGE,
                        Map.of("end", ParamValue.of(end)), Optional.of(scope)));
    }

    private static Rule loseRule(String triggerId, Scope scope) {
        return new Rule(TriggerSpec.of(triggerId),
                new EffectSpec(CoreCatalog.EFFECT_LOSE_CHALLENGE, Map.of(), Optional.of(scope)));
    }

    private Engine engineFor(Rule... rules) {
        return new Engine(new Challenge(List.of(rules), List.of()), registries);
    }

    private static void finish(Engine engine, String player) {
        engine.onEvent(GameEvent.of("trigger.game_beaten", player));
    }

    private static List<String> finishers(Engine engine) {
        return engine.completions().stream().map(Completion::playerId).toList();
    }

    // ---- the four shapes ----

    @Test
    void everyPlayerOnFirstCompletionWinsForTheWholeGroupAtOnce() {
        Engine engine = engineFor(winRule("trigger.game_beaten",
                Scope.EVERY_PLAYER, CoreCatalog.END_ON_FIRST_COMPLETION));
        engine.updateRoster(List.of("alice", "bob"));

        finish(engine, "alice");

        assertEquals(RunOutcome.WIN, engine.outcome());
        assertTrue(engine.winsTogether(), "an every-player win is shared by the group");
        assertEquals(List.of("alice"), finishers(engine),
                "the completion is still credited to whoever triggered it");
    }

    @Test
    void everyPlayerAfterAllCompleteWaitsForEachPlayerIndividually() {
        Engine engine = engineFor(winRule("trigger.game_beaten",
                Scope.EVERY_PLAYER, CoreCatalog.END_AFTER_ALL_COMPLETE));
        engine.updateRoster(List.of("alice", "bob"));

        finish(engine, "alice");
        assertEquals(RunOutcome.ONGOING, engine.outcome(),
                "one player finishing must not finish it for everybody");

        // Finishing takes a player out of survival, so the adapter reports a
        // roster without them on the next tick.
        engine.updateRoster(List.of("bob"));
        assertEquals(RunOutcome.ONGOING, engine.outcome());

        finish(engine, "bob");
        engine.updateRoster(List.of());

        assertEquals(RunOutcome.WIN, engine.outcome());
        assertTrue(engine.winsTogether());
        assertEquals(List.of("alice", "bob"), finishers(engine));
    }

    @Test
    void perPlayerOnFirstCompletionIsARaceTheFirstFinisherEnds() {
        Engine engine = engineFor(winRule("trigger.game_beaten",
                Scope.PER_PLAYER, CoreCatalog.END_ON_FIRST_COMPLETION));
        engine.updateRoster(List.of("alice", "bob"));

        finish(engine, "bob");

        assertEquals(RunOutcome.WIN, engine.outcome());
        assertFalse(engine.winsTogether(), "a per-player win belongs to whoever finished");
        assertEquals(List.of("bob"), finishers(engine));
    }

    @Test
    void perPlayerAfterAllCompleteRecordsEveryFinishInOrderWithItsTime() {
        Engine engine = engineFor(winRule("trigger.game_beaten",
                Scope.PER_PLAYER, CoreCatalog.END_AFTER_ALL_COMPLETE));
        engine.updateRoster(List.of("alice", "bob"));

        engine.tick(100);
        finish(engine, "bob");
        engine.updateRoster(List.of("alice"));
        engine.tick(50);
        finish(engine, "alice");
        engine.updateRoster(List.of());

        assertEquals(RunOutcome.WIN, engine.outcome());
        assertFalse(engine.winsTogether());
        assertEquals(List.of("bob", "alice"), finishers(engine), "finishing order is the placing");
        assertEquals(List.of(100L, 150L),
                engine.completions().stream().map(Completion::atTick).toList(),
                "each finish keeps the clock reading it happened at");
    }

    // ---- elimination and the empty roster ----

    @Test
    void aScopedLossTakesOnePlayerOutWhileTheRunCarriesOn() {
        Engine engine = engineFor(loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice", "bob"));

        engine.onEvent(GameEvent.of("trigger.player_died", "alice"));

        assertEquals(RunOutcome.ONGOING, engine.outcome(), "one player out is not the run lost");
        assertEquals(Set.of("alice"), engine.eliminated());
    }

    @Test
    void aRunEveryoneHasBeenEliminatedFromIsLost() {
        Engine engine = engineFor(loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice", "bob"));

        engine.onEvent(GameEvent.of("trigger.player_died", "alice"));
        engine.updateRoster(List.of("bob"));
        engine.onEvent(GameEvent.of("trigger.player_died", "bob"));
        engine.updateRoster(List.of());

        assertEquals(RunOutcome.LOSS, engine.outcome());
    }

    @Test
    void anEveryPlayerLossStillEndsTheRunForEverybody() {
        Engine engine = engineFor(loseRule("trigger.player_died", Scope.EVERY_PLAYER));
        engine.updateRoster(List.of("alice", "bob"));

        engine.onEvent(GameEvent.of("trigger.player_died", "alice"));
        engine.updateRoster(List.of());

        assertEquals(RunOutcome.LOSS, engine.outcome());
        assertEquals(Set.of("alice", "bob"), engine.eliminated());
    }

    @Test
    void anEmptyRosterDecidesNothingBeforeAnybodyHasFinishedOrGoneOut() {
        Engine engine = engineFor(winRule("trigger.game_beaten",
                Scope.PER_PLAYER, CoreCatalog.END_AFTER_ALL_COMPLETE));

        engine.updateRoster(List.of());

        assertEquals(RunOutcome.ONGOING, engine.outcome(),
                "nobody online is not the same as everybody having finished");
    }

    @Test
    void aRosterThatEmptiesWithOneFinisherAndOneCasualtyIsStillAWin() {
        Engine engine = engineFor(
                winRule("trigger.game_beaten", Scope.PER_PLAYER, CoreCatalog.END_AFTER_ALL_COMPLETE),
                loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice", "bob"));

        engine.onEvent(GameEvent.of("trigger.player_died", "bob"));
        engine.updateRoster(List.of("alice"));
        finish(engine, "alice");
        engine.updateRoster(List.of());

        assertEquals(RunOutcome.WIN, engine.outcome(), "somebody finished, so the run was won");
        assertEquals(List.of("alice"), finishers(engine));
        assertEquals(Set.of("bob"), engine.eliminated());
    }

    // ---- rejoining ----

    /**
     * The adapter cannot take a player out of the roster in the same instant it
     * eliminates them: it sees the outcome, then moves them to spectator, and
     * only the next roster report leaves them out. So an elimination has to
     * survive being told the player is still playing, or every elimination
     * undoes itself on the very next tick and nobody is ever knocked out.
     */
    @Test
    void anEliminationSurvivesTheRosterReportThatStillListsThePlayer() {
        Engine engine = engineFor(loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice", "bob"));

        engine.onEvent(GameEvent.of("trigger.player_died", "alice"));
        // The tick the death happened on still reports alice as playing.
        engine.updateRoster(List.of("alice", "bob"));

        assertEquals(Set.of("alice"), engine.eliminated(),
                "the elimination must not be cleared before she has actually left");
    }

    @Test
    void aSoloRunEndsWhenItsOnlyPlayerIsEliminated() {
        Engine engine = engineFor(loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice"));

        engine.onEvent(GameEvent.of("trigger.player_died", "alice"));
        engine.updateRoster(List.of("alice"));
        assertEquals(RunOutcome.ONGOING, engine.outcome(), "she has not left the roster yet");

        // The adapter has now moved her to spectator, so she drops out.
        engine.updateRoster(List.of());

        assertEquals(RunOutcome.LOSS, engine.outcome());
    }

    @Test
    void rejoiningTheRosterClearsARecordedLoss() {
        Engine engine = engineFor(loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice", "bob"));

        engine.onEvent(GameEvent.of("trigger.player_died", "alice"));
        assertEquals(Set.of("alice"), engine.eliminated());

        // She is moved to spectator and drops out of the roster, then puts
        // herself back into survival.
        engine.updateRoster(List.of("bob"));
        engine.updateRoster(List.of("alice", "bob"));

        assertTrue(engine.eliminated().isEmpty(), "back in survival is back in play");
        assertEquals(RunOutcome.ONGOING, engine.outcome());
    }

    @Test
    void anEliminatedPlayerWhoComesBackCanStillWin() {
        Engine engine = engineFor(
                winRule("trigger.game_beaten", Scope.PER_PLAYER, CoreCatalog.END_ON_FIRST_COMPLETION),
                loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice"));

        engine.onEvent(GameEvent.of("trigger.player_died", "alice"));
        engine.updateRoster(List.of("alice"));
        finish(engine, "alice");

        assertEquals(RunOutcome.WIN, engine.outcome());
        assertEquals(List.of("alice"), finishers(engine));
    }

    @Test
    void finishingTwiceDoesNotPlaceTwice() {
        Engine engine = engineFor(winRule("trigger.game_beaten",
                Scope.PER_PLAYER, CoreCatalog.END_AFTER_ALL_COMPLETE));
        engine.updateRoster(List.of("alice", "bob"));

        finish(engine, "alice");
        engine.tick(200);
        finish(engine, "alice");

        assertEquals(List.of("alice"), finishers(engine));
        assertEquals(0L, engine.completions().getFirst().atTick(), "the first finish is the one kept");
    }

    // ---- scope as a gate on who may finish ----

    @Test
    void aSpecificPlayerWinConditionDoesNotCountForAnybodyElse() {
        Engine engine = engineFor(winRule("trigger.game_beaten",
                Scope.players("alice"), CoreCatalog.END_ON_FIRST_COMPLETION));
        engine.updateRoster(List.of("alice", "bob"));

        finish(engine, "bob");
        assertEquals(RunOutcome.ONGOING, engine.outcome());
        assertTrue(engine.completions().isEmpty());

        finish(engine, "alice");
        assertEquals(RunOutcome.WIN, engine.outcome());
        assertEquals(List.of("alice"), finishers(engine));
    }

    @Test
    void aPlayerlessTriggerCompletesForTheWholeRoster() {
        Engine engine = engineFor(new Rule(
                new TriggerSpec("trigger.fixed_interval",
                        Map.of("seconds", ParamValue.of(600)), Optional.empty()),
                new EffectSpec(CoreCatalog.EFFECT_WIN_CHALLENGE,
                        Map.of("end", ParamValue.of(CoreCatalog.END_ON_FIRST_COMPLETION)),
                        Optional.of(Scope.PER_PLAYER))));
        engine.updateRoster(List.of("alice", "bob"));

        engine.onEvent(GameEvent.playerless("trigger.fixed_interval",
                Map.of("seconds", ParamValue.of(600))));

        assertEquals(RunOutcome.WIN, engine.outcome());
        assertEquals(Set.of("alice", "bob"), Set.copyOf(finishers(engine)),
                "nobody triggered it, so it completes for everyone still playing");
    }

    // ---- the modifier gate ----

    @Test
    void modifiersStopApplyingToAPlayerOutsideTheRoster() {
        Engine engine = new Engine(new Challenge(List.of(),
                List.of(Modifier.of("modifier.keep_inventory"))), registries);
        engine.updateRoster(List.of("alice"));

        assertEquals(1, engine.activeModifiersFor("alice").size());
        assertTrue(engine.activeModifiersFor("bob").isEmpty(), "bob is not in the run");

        engine.updateRoster(List.of("bob"));

        assertTrue(engine.activeModifiersFor("alice").isEmpty(), "alice left the run");
        assertEquals(1, engine.activeModifiersFor("bob").size(), "bob came back into it");
    }

    /**
     * A playerless modifier is in force for the run rather than for anybody, so
     * the adapter asks about it under an empty player id. That id is never in
     * the roster, so gating it the way a scoped modifier is gated would switch
     * every world-level modifier off the moment a roster was first reported.
     */
    @Test
    void aPlayerlessModifierIsNotGatedByTheRoster() {
        Engine engine = new Engine(new Challenge(List.of(),
                List.of(new Modifier("modifier.scale_hostile_mobs",
                        Map.of("multiplier", ParamValue.of(2.0)), Optional.empty()))), registries);
        engine.updateRoster(List.of("alice"));

        assertEquals(1, engine.activeModifiersFor("").size(),
                "the adapter's global lookup must still find it");
        assertEquals(1, engine.activeModifiersFor("alice").size());
    }

    @Test
    void anEngineNoAdapterHasReportedARosterToEnforcesModifiersNormally() {
        Engine engine = new Engine(new Challenge(List.of(),
                List.of(Modifier.of("modifier.keep_inventory"))), registries);

        assertEquals(1, engine.activeModifiersFor("alice").size(),
                "no roster report yet is not the same as an empty roster");
    }

    // ---- validation ----

    @Test
    void anUnknownEndValueIsRejectedRatherThanQuietlyDefaulted() {
        Challenge challenge = new Challenge(
                List.of(winRule("trigger.game_beaten", Scope.PER_PLAYER, "whenever_really")),
                List.of());

        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> new Engine(challenge, registries));

        assertTrue(rejection.getMessage().contains("must be one of"), rejection.getMessage());
        assertTrue(rejection.getMessage().contains("whenever_really"), rejection.getMessage());
    }

    @Test
    void theEndParameterIsRequired() {
        Challenge challenge = new Challenge(
                List.of(new Rule(TriggerSpec.of("trigger.game_beaten"),
                        new EffectSpec(CoreCatalog.EFFECT_WIN_CHALLENGE, Map.of(),
                                Optional.of(Scope.PER_PLAYER)))),
                List.of());

        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> new Engine(challenge, registries));

        assertTrue(rejection.getMessage().contains("missing required parameter 'end'"),
                rejection.getMessage());
    }

    @Test
    void neitherRunControlEffectIsEverDispatchedToTheAdapter() {
        Engine engine = engineFor(
                winRule("trigger.game_beaten", Scope.PER_PLAYER, CoreCatalog.END_AFTER_ALL_COMPLETE),
                loseRule("trigger.player_died", Scope.PER_PLAYER));
        engine.updateRoster(List.of("alice"));

        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.game_beaten", "alice")));
        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.player_died", "alice")));
    }
}
