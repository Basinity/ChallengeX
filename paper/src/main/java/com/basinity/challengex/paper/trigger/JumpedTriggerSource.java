package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * {@code trigger.jumped}: a player jumped.
 *
 * <p>Fabric needed a Mixin and a hop back onto the server thread for this;
 * Paper has an event for it, which is one of the places the Bukkit side is
 * simply better served.
 */
public final class JumpedTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJumped(PlayerJumpEvent event) {
        context().emit(GameEvent.of("trigger.jumped", event.getPlayer().getName()));
    }
}
