package com.basinity.challengex.core.engine;

import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.registry.Registries;
import java.util.List;
import java.util.Objects;

/**
 * One active run: the seam the platform feeds. It owns the {@link Engine} for a
 * loaded challenge and the {@link EffectExecutor} that carries effects out to
 * the game. The platform builds an abstract {@link GameEvent} from a real game
 * happening and calls {@link #handle}; the run dispatches the event and pumps
 * every resulting command to the executor. Effects consumed by the engine
 * itself (lose-challenge) never reach the executor.
 */
public final class ChallengeRun {

    private final Engine engine;
    private final EffectExecutor executor;

    public ChallengeRun(Challenge challenge, Registries registries, EffectExecutor executor) {
        this.engine = new Engine(challenge, registries);
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    /** Dispatches an event and executes the effects it fires. */
    public void handle(GameEvent event) {
        for (EffectCommand command : engine.onEvent(event)) {
            executor.execute(command);
        }
    }

    /** Advances the run's clock; modifier expiry is measured against it. */
    public void tick(long ticks) {
        engine.tick(ticks);
    }

    public long elapsedTicks() {
        return engine.elapsedTicks();
    }

    public List<Modifier> activeModifiersFor(String playerId) {
        return engine.activeModifiersFor(playerId);
    }

    public RunOutcome outcome() {
        return engine.outcome();
    }
}
