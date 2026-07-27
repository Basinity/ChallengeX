package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.core.engine.GameEvent;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * {@code trigger.game_beaten}: a player beat the game, leaving the End through
 * the exit portal.
 *
 * <p>Fabric rides the credits roll itself, which Bukkit does not expose. This
 * rides the portal traversal that causes it instead: going through an end
 * portal outward from the End is the one path to the credits, and the exit
 * portal only opens once the dragon is down. The two fire at effectively the
 * same moment, one just side of the other.
 *
 * <p>Direction is what separates beating the game from starting the fight, so
 * only travel out of an End world counts. The environment is checked rather
 * than the world's id, since a server can have more than one End.
 */
public final class GameBeatenTriggerSource extends EventTriggerSource {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameBeaten(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            return;
        }
        if (event.getFrom().getWorld().getEnvironment() != World.Environment.THE_END) {
            return;
        }
        context().emit(GameEvent.of("trigger.game_beaten", event.getPlayer().getName()));
    }
}
