package com.basinity.challengex.core.engine;

/**
 * A run's lifecycle stage, distinct from its {@link RunOutcome}. A loaded
 * challenge waits in {@link #NOT_STARTED} until it is explicitly started;
 * {@link #RUNNING} and {@link #PAUSED} alternate while it plays; {@link
 * #FINISHED} is terminal, reached the moment the engine decides a win or a
 * loss, and the outcome is meaningful only then. A reset returns a run to
 * {@code NOT_STARTED} with a fresh clock and cleared goal progress.
 */
public enum RunState {
    NOT_STARTED,
    RUNNING,
    PAUSED,
    FINISHED
}
