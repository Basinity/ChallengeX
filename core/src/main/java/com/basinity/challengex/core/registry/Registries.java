package com.basinity.challengex.core.registry;

import java.util.Objects;

/**
 * The three catalogs bundled, so everything that needs them (validation, the
 * engine, preset import, the web-catalog export) takes one handle.
 */
public record Registries(Registry<TriggerDefinition> triggers,
                         Registry<EffectDefinition> effects,
                         Registry<ModifierDefinition> modifiers) {

    public Registries {
        Objects.requireNonNull(triggers, "triggers");
        Objects.requireNonNull(effects, "effects");
        Objects.requireNonNull(modifiers, "modifiers");
    }
}
