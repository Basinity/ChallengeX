package com.basinity.challengex.common.lifecycle;

import com.basinity.challengex.common.text.LineStyle;
import com.basinity.challengex.common.text.StyledLine;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.Completion;
import com.basinity.challengex.core.engine.RunOutcome;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What a finished run says: the on-screen title, the chat line stating the
 * outcome and the final time, the result lines, and who among the players is
 * being congratulated rather than told they lost.
 *
 * <p>How the results read follows how the win was awarded. A win shared by
 * everyone reports one group time, which is the moment the last player finished
 * rather than whenever the run happened to stop. A win kept by whoever finished
 * reports a placing, a name and a time per line, in the order they finished, and
 * splits the presentation: the players who finished are congratulated, everyone
 * else is told the run was lost.
 *
 * <p>Drawing this is each adapter's job: the title packet, the sound, and the
 * clickable control that prints the composition are all platform types.
 */
public record RunAnnouncement(boolean won, boolean together, String title, String chatLine,
                              List<StyledLine> results, Set<String> finishers) {

    public RunAnnouncement {
        results = List.copyOf(results);
        finishers = Set.copyOf(finishers);
    }

    public static RunAnnouncement of(ChallengeRun run) {
        boolean won = run.outcome() == RunOutcome.WIN;
        List<Completion> completions = run.completions();
        boolean together = run.winsTogether();

        String time = RunClock.format(groupTicks(run, completions));
        Set<String> finishers = new LinkedHashSet<>();
        completions.forEach(completion -> finishers.add(completion.playerId()));

        return new RunAnnouncement(won, together,
                won ? "Challenge Complete" : "Challenge Failed",
                (won ? "Challenge complete — " : "Challenge failed — ") + time,
                results(won, together, completions, run.eliminated()),
                finishers);
    }

    /**
     * Whether this player is being congratulated. Under a per-player win only
     * the finishers won; for everybody else the run ended without them, so they
     * are told as much.
     */
    public boolean celebrates(String playerName) {
        return won && (together || finishers.contains(playerName));
    }

    /**
     * The time the run is reported at: the moment the last player finished when
     * anybody did, so a group's time is the finish that completed it rather than
     * whenever the clock happened to stop afterwards. A run nobody finished
     * reports the clock itself.
     */
    private static long groupTicks(ChallengeRun run, List<Completion> completions) {
        return completions.isEmpty() ? run.elapsedTicks() : completions.getLast().atTick();
    }

    private static List<StyledLine> results(boolean won, boolean together,
            List<Completion> completions, Set<String> eliminated) {
        List<StyledLine> lines = new ArrayList<>();
        // A shared win has already been reported as one group time; listing the
        // same finishes again would say nothing new.
        if (!together && !completions.isEmpty()) {
            for (int place = 0; place < completions.size(); place++) {
                Completion completion = completions.get(place);
                lines.add(StyledLine.of("  " + (place + 1) + ". " + completion.playerId()
                        + " — " + RunClock.format(completion.atTick()),
                        place == 0 ? LineStyle.GOLD : LineStyle.GRAY));
            }
        }
        if (!eliminated.isEmpty()) {
            lines.add(StyledLine.of("  Out: " + String.join(", ", eliminated.stream().sorted().toList()),
                    LineStyle.GRAY));
        }
        if (won && together && completions.size() > 1) {
            lines.add(StyledLine.of("  Everyone finished.", LineStyle.GRAY));
        }
        return lines;
    }
}
