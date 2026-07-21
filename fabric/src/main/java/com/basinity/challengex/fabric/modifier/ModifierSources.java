package com.basinity.challengex.fabric.modifier;

import java.util.List;

/**
 * Every event-cancel modifier source the adapter registers, the modifier-side
 * counterpart of {@code TriggerSources}.
 */
public final class ModifierSources {

    private ModifierSources() {
    }

    public static List<ModifierSource> all() {
        return List.of(
                new DisableItemUseModifierSource(),
                new DisableInteractionModifierSource(),
                new BuffHostileMobsModifierSource(),
                new RandomizeDropsModifierSource());
    }
}
