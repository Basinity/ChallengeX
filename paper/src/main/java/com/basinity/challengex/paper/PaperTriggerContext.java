package com.basinity.challengex.paper;

import com.basinity.challengex.common.trigger.TriggerContext;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Paper's half of the trigger contract, the mirror of {@code
 * PaperEffectExecutor}: sources emit through it and read the active challenge's
 * configuration from it.
 *
 * <p>It holds a supplier rather than the run itself because sources register
 * once at plugin enable, while runs are swapped on a preset import. With no run
 * active, emitting is a no-op and nothing is configured, so sources idle rather
 * than fail.
 */
final class PaperTriggerContext implements TriggerContext {

    private final Supplier<ChallengeRun> activeRun;

    PaperTriggerContext(Supplier<ChallengeRun> activeRun) {
        this.activeRun = Objects.requireNonNull(activeRun, "activeRun");
    }

    @Override
    public void emit(GameEvent event) {
        ChallengeRun run = activeRun.get();
        if (run != null) {
            run.handle(event);
        }
    }

    @Override
    public List<ParamValue> configured(String triggerId, String paramName) {
        ChallengeRun run = activeRun.get();
        return run == null ? List.of() : run.challenge().triggerParamValues(triggerId, paramName);
    }

    @Override
    public long elapsedTicks() {
        ChallengeRun run = activeRun.get();
        return run == null ? 0 : run.elapsedTicks();
    }
}
