package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Base for the trigger sources backed by a real Bukkit event. Registering a
 * listener is the same six lines every time, so it lives here and a source is
 * left as its {@code @EventHandler} method and nothing else.
 *
 * <p>The context is held rather than passed into the handler because Bukkit
 * decides a handler's signature, so there is nowhere to thread it through.
 */
public abstract class EventTriggerSource implements TriggerSource, Listener {

    private TriggerContext context;
    private Plugin plugin;

    @Override
    public final void register(TriggerContext triggerContext, Plugin owner) {
        this.context = triggerContext;
        this.plugin = owner;
        owner.getServer().getPluginManager().registerEvents(this, owner);
    }

    /** The context to emit through. Never null once registered. */
    protected final TriggerContext context() {
        return context;
    }

    /**
     * Emits on the main thread. Most Bukkit events already arrive there and can
     * emit directly, but chat is delivered asynchronously, and the engine plus
     * everything an effect touches is main-thread-only.
     */
    protected final void emitOnMainThread(Runnable emit) {
        plugin.getServer().getScheduler().runTask(plugin, emit);
    }
}
