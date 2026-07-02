package com.basinity.challengex.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
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

class EngineTest {

    private final Registries registries = CoreCatalog.createRegistries();

    private Engine engineFor(Rule... rules) {
        return new Engine(new Challenge(List.of(rules), Optional.empty(), List.of()), registries);
    }

    @Test
    void matchingTriggerDispatchesPairedEffect() {
        Engine engine = engineFor(Rule.of("trigger.damage_taken", "effect.drain_hunger"));

        assertEquals(
                List.of(new EffectCommand("effect.drain_hunger", Map.of(),
                        EffectCommand.Target.player("alice"))),
                engine.onEvent(GameEvent.of("trigger.damage_taken", "alice")));
    }

    @Test
    void nonMatchingTriggerDispatchesNothing() {
        Engine engine = engineFor(Rule.of("trigger.damage_taken", "effect.drain_hunger"));

        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.block_broken", "alice")));
    }

    @Test
    void rulesStackFreelyOnTheSameTrigger() {
        Engine engine = engineFor(
                Rule.of("trigger.jump", "effect.damage"),
                Rule.of("trigger.jump", "effect.drain_hunger"));

        List<EffectCommand> commands = engine.onEvent(GameEvent.of("trigger.jump", "alice"));

        assertEquals(List.of("effect.damage", "effect.drain_hunger"),
                commands.stream().map(EffectCommand::effectId).toList());
    }

    @Test
    void triggerParamsActAsFilters() {
        Engine engine = engineFor(new Rule(
                new TriggerSpec("trigger.mob_killed",
                        Map.of("mob", ParamValue.of("minecraft:zombie")),
                        Optional.of(Scope.EVERY_PLAYER)),
                EffectSpec.of("effect.heal")));

        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.mob_killed", "alice",
                Map.of("mob", ParamValue.of("minecraft:skeleton")))));
        assertEquals(1, engine.onEvent(GameEvent.of("trigger.mob_killed", "alice",
                Map.of("mob", ParamValue.of("minecraft:zombie")))).size());
    }

    @Test
    void specificPlayerTriggerWatchesOnlyThosePlayers() {
        Engine engine = engineFor(new Rule(
                new TriggerSpec("trigger.jump", Map.of(), Optional.of(Scope.players("alice"))),
                EffectSpec.of("effect.heal")));

        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.jump", "bob")));
        assertEquals(1, engine.onEvent(GameEvent.of("trigger.jump", "alice")).size());
    }

    @Test
    void everyPlayerEffectTargetsAllPlayers() {
        Engine engine = engineFor(new Rule(
                TriggerSpec.of("trigger.player_death"),
                new EffectSpec("effect.drop_inventory", Map.of(), Optional.of(Scope.EVERY_PLAYER))));

        assertEquals(List.of(new EffectCommand("effect.drop_inventory", Map.of(),
                        EffectCommand.Target.ALL_PLAYERS)),
                engine.onEvent(GameEvent.of("trigger.player_death", "alice")));
    }

    @Test
    void specificPlayerEffectTargetsChosenPlayers() {
        Engine engine = engineFor(new Rule(
                TriggerSpec.of("trigger.player_death"),
                new EffectSpec("effect.heal", Map.of(), Optional.of(Scope.players("carol", "dave")))));

        assertEquals(List.of(new EffectCommand("effect.heal", Map.of(),
                        new EffectCommand.Target.Players(Set.of("carol", "dave")))),
                engine.onEvent(GameEvent.of("trigger.player_death", "alice")));
    }

    @Test
    void perPlayerEffectOnPlayerlessTriggerFallsBackToEveryone() {
        Engine engine = engineFor(new Rule(
                TriggerSpec.playerless("trigger.weather_change"),
                EffectSpec.of("effect.heal")));

        assertEquals(List.of(new EffectCommand("effect.heal", Map.of(),
                        EffectCommand.Target.ALL_PLAYERS)),
                engine.onEvent(GameEvent.playerless("trigger.weather_change")));
    }

    @Test
    void playerlessEffectTargetsEveryone() {
        Engine engine = engineFor(new Rule(
                TriggerSpec.of("trigger.jump"),
                new EffectSpec("effect.change_weather",
                        Map.of("value", ParamValue.of("clear")), Optional.empty())));

        assertEquals(List.of(new EffectCommand("effect.change_weather",
                        Map.of("value", ParamValue.of("clear")), EffectCommand.Target.ALL_PLAYERS)),
                engine.onEvent(GameEvent.of("trigger.jump", "alice")));
    }

    @Test
    void specificPlayerTriggerIgnoresPlayerlessEvents() {
        Engine engine = engineFor(new Rule(
                new TriggerSpec("trigger.jump", Map.of(), Optional.of(Scope.players("alice"))),
                EffectSpec.of("effect.heal")));

        assertEquals(List.of(), engine.onEvent(GameEvent.playerless("trigger.jump")));
    }

    @Test
    void loseChallengeEffectEndsRunAsLossWithoutDispatching() {
        Engine engine = engineFor(new Rule(
                TriggerSpec.of("trigger.player_death"),
                EffectSpec.playerless("effect.lose_challenge")));

        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.player_death", "alice")));
        assertEquals(RunOutcome.LOSS, engine.outcome());
    }

    @Test
    void noDispatchAfterTheRunEnds() {
        Engine engine = engineFor(
                new Rule(TriggerSpec.of("trigger.player_death"),
                        EffectSpec.playerless("effect.lose_challenge")),
                Rule.of("trigger.jump", "effect.heal"));
        engine.onEvent(GameEvent.of("trigger.player_death", "alice"));

        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.jump", "alice")));
    }

    @Test
    void unknownIdsAreRejectedAtConstruction() {
        IllegalArgumentException rejection = assertThrows(IllegalArgumentException.class,
                () -> engineFor(Rule.of("trigger.does_not_exist", "effect.heal")));

        assertTrue(rejection.getMessage().contains("trigger.does_not_exist"));
    }
}
