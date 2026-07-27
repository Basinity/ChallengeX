package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.common.modifier.ModifierContext;

/**
 * A modifier enforced by cancelling a game action outright rather than by
 * continuous per-tick state, the modifier-side mirror of {@code
 * TriggerSource}. {@link ModifierSources} registers them all once at mod init.
 */
public interface ModifierSource {

    void register(ModifierContext context);
}
