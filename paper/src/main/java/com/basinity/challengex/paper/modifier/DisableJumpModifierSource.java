package com.basinity.challengex.paper.modifier;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * {@code modifier.disable_jump}: the player cannot jump.
 *
 * <p>Fabric zeroes the jump-strength attribute, because it has no jump event to
 * stop. Paper does, so this cancels the jump outright, which is the same real
 * prevention with nothing left on the player to leak if the modifier stops
 * while they are offline.
 */
public final class DisableJumpModifierSource extends EventModifierSource {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJump(PlayerJumpEvent event) {
        if (isActive(event.getPlayer(), "modifier.disable_jump")) {
            event.setCancelled(true);
        }
    }
}
