package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.model.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Fabric's implementation of {@link ModifierContext}, the modifier-side mirror
 * of {@code FabricTriggerContext}. It holds a supplier rather than the run
 * itself because enforcers and sources register once at mod init, while runs
 * come and go with the server and are swapped on a preset import.
 */
public final class FabricModifierContext implements ModifierContext {

    private final Supplier<ChallengeRun> activeRun;

    public FabricModifierContext(Supplier<ChallengeRun> activeRun) {
        this.activeRun = Objects.requireNonNull(activeRun, "activeRun");
    }

    @Override
    public List<Modifier> activeModifiersFor(String playerId) {
        ChallengeRun run = activeRun.get();
        return run == null ? List.of() : run.activeModifiersFor(playerId);
    }
}
