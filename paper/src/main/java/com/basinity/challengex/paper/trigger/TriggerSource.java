package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import org.bukkit.plugin.Plugin;

/**
 * Watches the game for one catalog trigger and emits its abstract event. One
 * source per trigger id, the mirror of the one-handler-per-id effect side;
 * {@link TriggerSources} registers them all once at plugin enable.
 *
 * <p>Unlike the Fabric side, registering needs the plugin: Bukkit listeners and
 * scheduled tasks are both owned by a plugin rather than registered globally.
 */
public interface TriggerSource {

    /** Hooks the game up to the context. Called once, at plugin enable. */
    void register(TriggerContext context, Plugin plugin);
}
