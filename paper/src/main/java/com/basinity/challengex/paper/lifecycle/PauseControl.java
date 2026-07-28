package com.basinity.challengex.paper.lifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * The two mechanisms a real pause needs. Freezing the server tick manager stops
 * mobs, block ticks, time, and weather; but a tick freeze does not stop a
 * connected client's own movement packets, so a paused player is teleported back
 * to their frozen position every tick. The scheduler keeps running while the
 * game is frozen, which is what lets that hold work at all.
 *
 * <p>Only this class calls {@code setFrozen}, and it unfreezes only what it
 * froze, so it never fights a manually issued {@code /tick freeze}.
 */
public final class PauseControl {

    private final Map<UUID, Location> heldPositions = new HashMap<>();
    private boolean frozen;

    /** Freezes world simulation and captures where every player stands. */
    public void freeze(Server server) {
        server.getServerTickManager().setFrozen(true);
        frozen = true;
        heldPositions.clear();
        for (Player player : server.getOnlinePlayers()) {
            heldPositions.put(player.getUniqueId(), player.getLocation());
        }
    }

    /** Unfreezes, but only if this control was the one that froze. */
    public void unfreeze(Server server) {
        if (frozen) {
            server.getServerTickManager().setFrozen(false);
            frozen = false;
        }
        heldPositions.clear();
    }

    /**
     * Holds every player at the position they were frozen at, teleporting back
     * anyone whose client moved them. A player who joined mid-pause is captured
     * where they land and held from there.
     */
    public void holdPlayers(Server server) {
        for (Player player : server.getOnlinePlayers()) {
            Location held = heldPositions.get(player.getUniqueId());
            if (held == null) {
                heldPositions.put(player.getUniqueId(), player.getLocation());
                continue;
            }
            Location now = player.getLocation();
            if (now.getX() != held.getX() || now.getY() != held.getY() || now.getZ() != held.getZ()) {
                // Their own facing is kept: snapping the camera back as well
                // would make a pause feel like a malfunction rather than a hold.
                Location target = held.clone();
                target.setYaw(now.getYaw());
                target.setPitch(now.getPitch());
                player.teleport(target);
            }
        }
    }
}
