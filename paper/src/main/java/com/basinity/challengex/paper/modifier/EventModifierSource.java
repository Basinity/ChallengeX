package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierContext;
import com.basinity.challengex.core.model.Modifier;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Base for the modifier sources that work by cancelling a game action. The
 * listener registration is the same every time, and so is asking whether a
 * modifier is in force for the player about to act.
 */
public abstract class EventModifierSource implements ModifierSource, Listener {

    private ModifierContext context;

    @Override
    public final void register(ModifierContext modifierContext, Plugin plugin) {
        this.context = modifierContext;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        onRegistered(plugin);
    }

    /**
     * Setup beyond the listener, for a source that also needs a scheduled task.
     * Most cancel an event and nothing more, so most need not override this.
     */
    protected void onRegistered(Plugin plugin) {
    }

    protected final ModifierContext context() {
        return context;
    }

    /** The modifier in force for this player, if it is. */
    protected final Optional<Modifier> find(Player player, String modifierId) {
        return context.find(player.getName(), modifierId);
    }

    protected final boolean isActive(Player player, String modifierId) {
        return find(player, modifierId).isPresent();
    }
}
