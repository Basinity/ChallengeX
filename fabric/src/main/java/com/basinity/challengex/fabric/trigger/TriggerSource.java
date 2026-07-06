package com.basinity.challengex.fabric.trigger;

/**
 * Watches the game for one catalog trigger and emits its abstract event. One
 * source per trigger id, the mirror of the one-handler-per-id effect side;
 * {@link TriggerSources} registers them all once at mod init.
 *
 * <p>A source registers whatever it needs (a Fabric event, a server tick hook,
 * a Mixin callback) and keeps its own state, such as the previous per-player
 * value a rising-edge poll compares against.
 */
@FunctionalInterface
public interface TriggerSource {

    /** Hooks the game up to the context. Called once, at mod init. */
    void register(TriggerContext context);
}
