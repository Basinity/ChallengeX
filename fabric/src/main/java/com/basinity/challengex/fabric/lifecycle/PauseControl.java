package com.basinity.challengex.fabric.lifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * The two mechanisms a real pause needs. Freezing the vanilla tick rate manager
 * stops mobs, block ticks, time, and weather; but tick-freeze does not stop a
 * connected client's own movement packets, so a paused player is teleported
 * back to their frozen position every server tick (the server tick loop, and so
 * this hold, keeps running while the game is frozen).
 *
 * <p>Only this class calls {@code setFrozen}, and it unfreezes only what it
 * froze, so it never fights a manually issued {@code /tick freeze}.
 */
public final class PauseControl {

    private final Map<UUID, double[]> heldPositions = new HashMap<>();
    private boolean frozen;

    /** Freezes world simulation and captures where every player stands. */
    public void freeze(MinecraftServer server) {
        server.tickRateManager().setFrozen(true);
        frozen = true;
        heldPositions.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            heldPositions.put(player.getUUID(), positionOf(player));
        }
    }

    /** Unfreezes, but only if this control was the one that froze. */
    public void unfreeze(MinecraftServer server) {
        if (frozen) {
            server.tickRateManager().setFrozen(false);
            frozen = false;
        }
        heldPositions.clear();
    }

    /**
     * Holds every player at the position they were frozen at, teleporting back
     * anyone whose client moved them. A player who joined mid-pause is captured
     * where they land and held from there.
     */
    public void holdPlayers(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            double[] held = heldPositions.get(player.getUUID());
            if (held == null) {
                heldPositions.put(player.getUUID(), positionOf(player));
                continue;
            }
            if (player.getX() != held[0] || player.getY() != held[1] || player.getZ() != held[2]) {
                player.teleportTo(held[0], held[1], held[2]);
            }
        }
    }

    private static double[] positionOf(ServerPlayer player) {
        return new double[] {player.getX(), player.getY(), player.getZ()};
    }
}
