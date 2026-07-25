package com.basinity.challengex.common.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.basinity.challengex.common.text.LineStyle;
import com.basinity.challengex.common.text.StyledLine;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.core.registry.Registries;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RunAnnouncementTest {

    private final Registries registries = CoreCatalog.createRegistries();

    private ChallengeRun raceRun() {
        return new ChallengeRun(new Challenge(List.of(
                new Rule(TriggerSpec.of("trigger.game_beaten"),
                        new EffectSpec(CoreCatalog.EFFECT_WIN_CHALLENGE,
                                Map.of("end", ParamValue.of(CoreCatalog.END_AFTER_ALL_COMPLETE)),
                                Optional.of(Scope.PER_PLAYER))),
                new Rule(TriggerSpec.of("trigger.player_died"),
                        new EffectSpec(CoreCatalog.EFFECT_LOSE_CHALLENGE, Map.of(),
                                Optional.of(Scope.PER_PLAYER)))),
                List.of()), registries, command -> { });
    }

    private static List<String> textOf(List<StyledLine> lines) {
        return lines.stream().map(StyledLine::text).toList();
    }

    @Test
    @DisplayName("a race reports a placing per finisher, the winner picked out in gold")
    void perPlayerWinListsPlacings() {
        ChallengeRun run = raceRun();
        run.start();
        run.updateRoster(List.of("Pix", "Kettu", "Basinity"));
        run.tick(400);
        run.handle(GameEvent.of("trigger.game_beaten", "Kettu"));
        run.tick(500);
        run.handle(GameEvent.of("trigger.game_beaten", "Pix"));
        run.handle(GameEvent.of("trigger.player_died", "Basinity"));
        run.updateRoster(List.of());

        RunAnnouncement announcement = RunAnnouncement.of(run);

        assertTrue(announcement.won());
        assertFalse(announcement.together());
        assertEquals("Challenge Complete", announcement.title());
        // The reported time is the last finish (900 ticks), not the clock.
        assertEquals("Challenge complete — 45s", announcement.chatLine());
        assertEquals(List.of(
                "  1. Kettu — 20s",
                "  2. Pix — 45s",
                "  Out: Basinity"), textOf(announcement.results()));
        assertEquals(LineStyle.GOLD, announcement.results().getFirst().style());
        assertEquals(LineStyle.GRAY, announcement.results().get(1).style());
    }

    @Test
    @DisplayName("under a per-player win only the finishers are congratulated")
    void onlyFinishersCelebrate() {
        ChallengeRun run = raceRun();
        run.start();
        run.updateRoster(List.of("Pix", "Basinity"));
        run.tick(400);
        run.handle(GameEvent.of("trigger.game_beaten", "Pix"));
        run.handle(GameEvent.of("trigger.player_died", "Basinity"));
        run.updateRoster(List.of());

        RunAnnouncement announcement = RunAnnouncement.of(run);

        assertTrue(announcement.celebrates("Pix"));
        assertFalse(announcement.celebrates("Basinity"));
        // Somebody watching who was never in the run is not congratulated either.
        assertFalse(announcement.celebrates("Kettu"));
    }

    @Test
    @DisplayName("a lost run tells everyone it was lost, nobody celebrating")
    void lossCelebratesNobody() {
        ChallengeRun run = new ChallengeRun(new Challenge(List.of(),
                List.of(new Modifier(CoreCatalog.MODIFIER_TIME_LIMIT,
                        Map.of("minutes", ParamValue.of(1L)), Optional.empty()))),
                registries, command -> { });
        run.start();
        run.updateRoster(List.of("Pix"));
        run.tick(1200);

        RunAnnouncement announcement = RunAnnouncement.of(run);

        assertFalse(announcement.won());
        assertEquals("Challenge Failed", announcement.title());
        assertTrue(announcement.chatLine().startsWith("Challenge failed — "));
        assertFalse(announcement.celebrates("Pix"));
    }
}
