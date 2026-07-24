package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.Challenge;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable capture of a whole run: its composition and everything needed to
 * resume it exactly, being the lifecycle {@link RunState}, the clock's elapsed
 * ticks, the decided {@link RunOutcome}, who finished and in what order, and who
 * is out. Per-player rule state is deliberately not captured; it stays
 * ephemeral, as it already is for a mid-run joiner, and neither is the roster,
 * which the adapter reports afresh from game mode on the next tick.
 *
 * <p>Finishing order is the placing a race reports, so completions are a list
 * and their order is part of the format. A player may appear only once.
 *
 * <p>The snapshot carries its own version, independent of the preset schema, so
 * a snapshot written by a newer or older build is rejected rather than misread.
 */
public record RunSnapshot(int snapshotVersion, Challenge challenge, RunState state,
        long elapsedTicks, RunOutcome outcome, List<Completion> completions,
        Set<String> eliminated) {

    /** The current snapshot format version. */
    public static final int SNAPSHOT_VERSION = 2;

    public RunSnapshot {
        Objects.requireNonNull(challenge, "challenge");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(outcome, "outcome");
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks must not be negative");
        }
        completions = List.copyOf(completions);
        Set<String> finishers = new LinkedHashSet<>();
        for (Completion completion : completions) {
            if (!finishers.add(completion.playerId())) {
                throw new IllegalArgumentException(
                        "A player finishes once: " + completion.playerId());
            }
        }
        eliminated = Set.copyOf(eliminated);
    }
}
