package com.basinity.challengex.common.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.common.text.LineStyle;
import com.basinity.challengex.common.text.StyledLine;
import com.basinity.challengex.core.engine.RunState;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ChallengeSummaryTest {

    private static List<String> textOf(List<StyledLine> lines) {
        return lines.stream().map(StyledLine::text).toList();
    }

    @Test
    @DisplayName("an empty challenge still names itself and says both sections are empty")
    void emptyChallenge() {
        List<StyledLine> lines = ChallengeSummary.describe(
                new Challenge(List.of(), List.of()), null, RunState.NOT_STARTED);

        assertEquals(List.of(
                "Challenge active challenge  (not started)",
                "Rules: none",
                "Modifiers: none"), textOf(lines));
        assertEquals(LineStyle.GOLD, lines.getFirst().style());
    }

    @Test
    @DisplayName("a preset name is quoted, and the run state is labelled in words")
    void headerCarriesNameAndState() {
        List<StyledLine> lines = ChallengeSummary.describe(
                new Challenge(List.of(), List.of()), "speedrun", RunState.PAUSED);

        assertEquals("Challenge \"speedrun\"  (paused)", lines.getFirst().text());
    }

    @Test
    @DisplayName("a rule prints trigger, parameters, scope, then the effect")
    void ruleLine() {
        Rule rule = new Rule(
                TriggerSpec.of("trigger.mob_killed"),
                new EffectSpec("effect.apply_status_effect",
                        Map.of("effect", ParamValue.of("minecraft:poison"),
                                "duration", ParamValue.of(200L)),
                        Optional.of(Scope.PER_PLAYER)));

        List<String> lines = textOf(ChallengeSummary.describe(
                new Challenge(List.of(rule), List.of()), null, RunState.RUNNING));

        assertTrue(lines.contains("Rules (1):"), lines.toString());
        assertTrue(lines.contains(
                "  - trigger.mob_killed [everyone]  ->  effect.apply_status_effect "
                        + "{duration=200, effect=minecraft:poison} [triggering player]"),
                lines.toString());
    }

    @Test
    @DisplayName("parameters read in name order, whatever order the map holds them in")
    void parametersAreSorted() {
        Modifier modifier = new Modifier("modifier.scale_hostile_mobs",
                Map.of("zeta", ParamValue.of(1L), "alpha", ParamValue.of(2L)),
                Optional.of(Scope.EVERY_PLAYER));

        List<String> lines = textOf(ChallengeSummary.describe(
                new Challenge(List.of(), List.of(modifier)), null, RunState.RUNNING));

        assertTrue(lines.contains("  - modifier.scale_hostile_mobs {alpha=2, zeta=1} [everyone]"),
                lines.toString());
    }

    @Test
    @DisplayName("a whole decimal drops its trailing zero rather than printing 2.0")
    void wholeDecimalsPrintWhole() {
        Modifier modifier = new Modifier("modifier.scale_hostile_mobs",
                Map.of("multiplier", ParamValue.of(2.0)),
                Optional.of(Scope.EVERY_PLAYER));

        List<String> lines = textOf(ChallengeSummary.describe(
                new Challenge(List.of(), List.of(modifier)), null, RunState.RUNNING));

        assertTrue(lines.contains("  - modifier.scale_hostile_mobs {multiplier=2} [everyone]"),
                lines.toString());
    }

    @Test
    @DisplayName("specific players are listed alphabetically, so the line is stable")
    void specificPlayersAreSorted() {
        Modifier modifier = new Modifier("modifier.disable_jump", Map.of(),
                Optional.of(Scope.players("zoe", "alice", "mia")));

        List<String> lines = textOf(ChallengeSummary.describe(
                new Challenge(List.of(), List.of(modifier)), null, RunState.RUNNING));

        assertTrue(lines.contains("  - modifier.disable_jump [alice, mia, zoe]"), lines.toString());
    }
}
