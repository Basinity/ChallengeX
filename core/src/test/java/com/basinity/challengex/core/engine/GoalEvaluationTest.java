package com.basinity.challengex.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Goal;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Registries;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GoalEvaluationTest {

    private final Registries registries = CoreCatalog.createRegistries();

    private Engine engineWithGoal(Goal goal, Rule... rules) {
        return new Engine(new Challenge(List.of(rules), Optional.of(goal), List.of()), registries);
    }

    private static Goal defeatDragon() {
        return new Goal("goal.kill_mob", Map.of("mob", ParamValue.of("minecraft:ender_dragon")));
    }

    @Test
    void defeatBossGoalWinsOnTheMatchingKill() {
        Engine engine = engineWithGoal(defeatDragon());

        engine.onEvent(GameEvent.of("trigger.mob_killed", "alice",
                Map.of("mob", ParamValue.of("minecraft:ender_dragon"))));

        assertEquals(RunOutcome.WIN, engine.outcome());
    }

    @Test
    void wrongMobDoesNotCompleteTheGoal() {
        Engine engine = engineWithGoal(defeatDragon());

        engine.onEvent(GameEvent.of("trigger.mob_killed", "alice",
                Map.of("mob", ParamValue.of("minecraft:pig"))));

        assertEquals(RunOutcome.ONGOING, engine.outcome());
    }

    @Test
    void obtainItemGoalCompletesFromCraftingAsWellAsPickup() {
        Goal elytra = new Goal("goal.obtain_item", Map.of("item", ParamValue.of("minecraft:elytra")));
        Engine engine = engineWithGoal(elytra);

        engine.onEvent(GameEvent.of("trigger.item_crafted", "alice",
                Map.of("item", ParamValue.of("minecraft:elytra"))));

        assertEquals(RunOutcome.WIN, engine.outcome());
    }

    @Test
    void winningStopsFurtherDispatch() {
        Engine engine = engineWithGoal(defeatDragon(), Rule.of("trigger.jumped", "effect.heal"));

        engine.onEvent(GameEvent.of("trigger.mob_killed", "alice",
                Map.of("mob", ParamValue.of("minecraft:ender_dragon"))));

        assertEquals(List.of(), engine.onEvent(GameEvent.of("trigger.jumped", "alice")));
    }
}
