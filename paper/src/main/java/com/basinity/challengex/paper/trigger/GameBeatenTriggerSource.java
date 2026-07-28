package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * {@code trigger.game_beaten}: a player beat the game, leaving the End through
 * the exit portal.
 *
 * <p>Fabric rides the credits roll itself. Paper has no credits event, and the
 * traversal is not portal travel either: leaving the End is a respawn, which is
 * why watching for a portal event caught nothing at all. Bukkit labels that
 * respawn {@code END_PORTAL}, and nothing else produces that label, so it is
 * the same moment the mod fires on and needs no filtering by world or direction.
 */
public final class GameBeatenTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR)
    public void onGameBeaten(PlayerRespawnEvent event) {
        if (event.getRespawnReason() != PlayerRespawnEvent.RespawnReason.END_PORTAL) {
            return;
        }
        context().emit(GameEvent.of("trigger.game_beaten", event.getPlayer().getName()));
    }
}
