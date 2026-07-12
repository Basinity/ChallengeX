package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.Challenge;
import java.util.Objects;
import java.util.Set;

/**
 * An immutable capture of a whole run: its composition and everything needed to
 * resume it exactly — the lifecycle {@link RunState}, the clock's elapsed ticks,
 * the decided {@link RunOutcome}, and which of a compound goal's requirements
 * are already met. Per-player rule state is deliberately not captured; it stays
 * ephemeral, as it already is for a mid-run joiner.
 *
 * <p>The snapshot carries its own version, independent of the preset schema, so
 * a snapshot written by a newer build is rejected rather than misread.
 */
public record RunSnapshot(int snapshotVersion, Challenge challenge, RunState state,
        long elapsedTicks, RunOutcome outcome, Set<Integer> goalProgress) {

    /** The current snapshot format version. */
    public static final int SNAPSHOT_VERSION = 1;

    public RunSnapshot {
        Objects.requireNonNull(challenge, "challenge");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(outcome, "outcome");
        if (elapsedTicks < 0) {
            throw new IllegalArgumentException("elapsedTicks must not be negative");
        }
        goalProgress = Set.copyOf(goalProgress);
    }
}
