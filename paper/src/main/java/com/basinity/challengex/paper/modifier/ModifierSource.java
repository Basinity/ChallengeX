package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierContext;
import org.bukkit.plugin.Plugin;

/**
 * A modifier enforced by cancelling a game action outright rather than by
 * continuous per-tick state, the modifier-side mirror of {@code TriggerSource}.
 * {@link ModifierSources} registers them all once at plugin enable.
 *
 * <p>Bukkit's cancellable events are why this adapter leans on sources where
 * Fabric needed Mixins: stopping an action is what an event cancel is for.
 */
public interface ModifierSource {

    void register(ModifierContext context, Plugin plugin);
}
