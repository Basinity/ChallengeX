package com.basinity.challengex.paper.effect;

import org.bukkit.Server;
import org.bukkit.World;

/**
 * Picks the world the world-level effects act on. The Fabric adapter gets this
 * for free, since the server's own weather and clock setters mean the overworld;
 * on Bukkit both are per-world, so the choice has to be made explicitly and is
 * made in one place.
 */
public final class Worlds {

    private Worlds() {
    }

    /** The main world, which is the overworld the server was started on. */
    public static World overworld(Server server) {
        return server.getWorlds().getFirst();
    }
}
