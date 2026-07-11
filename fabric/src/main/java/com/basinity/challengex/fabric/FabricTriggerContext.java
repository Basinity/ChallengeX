package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.GameEvent;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.fabric.trigger.TriggerContext;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Fabric's half of the trigger contract, the mirror of {@code
 * FabricEffectExecutor}: sources emit through it and read the active
 * challenge's configuration from it.
 *
 * <p>It holds a supplier rather than the run itself because sources register
 * once at mod init, while runs come and go with the server and are swapped on a
 * preset import. With no run active, emitting is a no-op and nothing is
 * configured, so sources idle rather than fail.
 */
final class FabricTriggerContext implements TriggerContext {

    private final Supplier<ChallengeRun> activeRun;

    FabricTriggerContext(Supplier<ChallengeRun> activeRun) {
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
