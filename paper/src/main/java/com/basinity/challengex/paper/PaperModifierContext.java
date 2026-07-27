package com.basinity.challengex.paper;

import com.basinity.challengex.common.modifier.ModifierContext;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.model.Modifier;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Paper's implementation of {@link ModifierContext}, the modifier-side mirror
 * of {@code PaperTriggerContext}. It holds a supplier rather than the run
 * itself because enforcers and sources register once at plugin enable, while
 * runs are swapped on a preset import.
 */
final class PaperModifierContext implements ModifierContext {

    private final Supplier<ChallengeRun> activeRun;

    PaperModifierContext(Supplier<ChallengeRun> activeRun) {
        this.activeRun = Objects.requireNonNull(activeRun, "activeRun");
    }

    @Override
    public List<Modifier> activeModifiersFor(String playerId) {
        ChallengeRun run = activeRun.get();
        return run == null ? List.of() : run.activeModifiersFor(playerId);
    }
}
