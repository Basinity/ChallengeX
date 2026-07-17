package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.registry.Registries;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * One active run: the seam the platform feeds, and the owner of the run's
 * lifecycle state. It holds the {@link Engine} for a loaded challenge and the
 * {@link EffectExecutor} that carries effects out to the game.
 *
 * <p>A loaded challenge waits in {@link RunState#NOT_STARTED}: events do
 * nothing, the clock stays put, and no modifier is in force. {@link #start()}
 * begins it, {@link #pause()}/{@link #resume()} suspend and continue it, and
 * {@link #reset()} rebuilds a fresh engine of the same challenge. The moment
 * the engine decides a win or a loss the run turns {@link RunState#FINISHED},
 * after which it, too, is inert until reset. Effects consumed by the engine
 * itself (lose-challenge) never reach the executor.
 */
public final class ChallengeRun {

    private final Challenge challenge;
    private final Registries registries;
    private final EffectExecutor executor;
    private Engine engine;
    private RunState state = RunState.NOT_STARTED;

    public ChallengeRun(Challenge challenge, Registries registries, EffectExecutor executor) {
        this.challenge = Objects.requireNonNull(challenge, "challenge");
        this.registries = Objects.requireNonNull(registries, "registries");
        this.executor = Objects.requireNonNull(executor, "executor");
        this.engine = new Engine(challenge, registries);
    }

    /**
     * Rebuilds a run from a saved snapshot: the engine is restored mid-run and
     * the lifecycle state is taken from the snapshot, so the run resumes exactly
     * where it left off rather than reloading as a fresh not-started import.
     */
    public static ChallengeRun restore(RunSnapshot snapshot, Registries registries,
            EffectExecutor executor) {
        ChallengeRun run = new ChallengeRun(snapshot.challenge(), registries, executor);
        run.engine = Engine.restore(snapshot.challenge(), registries,
                snapshot.elapsedTicks(), snapshot.outcome(), snapshot.goalProgress());
        run.state = snapshot.state();
        return run;
    }

    /** Captures the whole run — composition, state, clock, outcome, goal progress. */
    public RunSnapshot snapshot() {
        return new RunSnapshot(RunSnapshot.SNAPSHOT_VERSION, challenge, state,
                engine.elapsedTicks(), engine.outcome(), engine.goalProgress());
    }

    public RunState state() {
        return state;
    }

    /** Begins a loaded run; a no-op unless it is waiting to start. */
    public void start() {
        if (state == RunState.NOT_STARTED) {
            state = RunState.RUNNING;
        }
    }

    /** Suspends a running run, freezing its clock; a no-op otherwise. */
    public void pause() {
        if (state == RunState.RUNNING) {
            state = RunState.PAUSED;
        }
    }

    /** Continues a paused run; a no-op otherwise. */
    public void resume() {
        if (state == RunState.PAUSED) {
            state = RunState.RUNNING;
        }
    }

    /** Rebuilds the run from the same challenge, back to a fresh not-started state. */
    public void reset() {
        this.engine = new Engine(challenge, registries);
        this.state = RunState.NOT_STARTED;
    }

    /** Dispatches an event and executes the effects it fires, only while running. */
    public void handle(GameEvent event) {
        if (state != RunState.RUNNING) {
            return;
        }
        for (EffectCommand command : engine.onEvent(event)) {
            executor.execute(command);
        }
        syncFinished();
    }

    /** Advances the run's clock while running; a time limit ends the run when it runs out. */
    public void tick(long ticks) {
        if (state != RunState.RUNNING) {
            return;
        }
        engine.tick(ticks);
        syncFinished();
    }

    /** A run whose engine has decided a win or a loss is finished. */
    private void syncFinished() {
        if (engine.outcome() != RunOutcome.ONGOING) {
            state = RunState.FINISHED;
        }
    }

    /** The challenge being run; platform trigger sources read what it watches for. */
    public Challenge challenge() {
        return challenge;
    }

    public long elapsedTicks() {
        return engine.elapsedTicks();
    }

    /** What the run clock should read: remaining under a time limit, else elapsed. */
    public long displayTicks() {
        return engine.displayTicks();
    }

    /** The run's time budget in ticks, or empty when it has no time limit. */
    public OptionalLong timeLimitTicks() {
        return engine.timeLimitTicks();
    }

    /**
     * The modifiers in force for a player right now. A run that has not started
     * or has finished enforces nothing, so the platform tears every modifier
     * down when the run ends and applies none before it begins.
     */
    public List<Modifier> activeModifiersFor(String playerId) {
        if (state == RunState.NOT_STARTED || state == RunState.FINISHED) {
            return List.of();
        }
        return engine.activeModifiersFor(playerId);
    }

    public RunOutcome outcome() {
        return engine.outcome();
    }
}
