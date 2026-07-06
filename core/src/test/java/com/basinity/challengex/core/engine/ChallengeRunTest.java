package com.basinity.challengex.core.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Registries;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChallengeRunTest {

    private final Registries registries = CoreCatalog.createRegistries();

    private ChallengeRun runFor(EffectExecutor executor, Rule... rules) {
        return new ChallengeRun(
                new Challenge(List.of(rules), Optional.empty(), List.of()), registries, executor);
    }

    @Test
    void mobKilledFiresStatusEffectOnTheKiller() {
        List<EffectCommand> executed = new ArrayList<>();
        ChallengeRun run = runFor(executed::add, new Rule(
                TriggerSpec.of("trigger.mob_killed"),
                new EffectSpec("effect.apply_status_effect",
                        Map.of("effect", ParamValue.of("minecraft:poison"),
                                "duration", ParamValue.of(200L)),
                        Optional.of(com.basinity.challengex.core.model.Scope.PER_PLAYER))));

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
                EffectSpec.playerless("effect.lose_challenge")));

        run.handle(GameEvent.of("trigger.player_died", "alice"));

        assertTrue(executed.isEmpty());
        assertEquals(RunOutcome.LOSS, run.outcome());
    }
}
