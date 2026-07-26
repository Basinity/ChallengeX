package com.basinity.challengex.paper.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** Player-selection helpers shared by effect handlers. */
public final class Players {

    private Players() {
    }

    /**
     * A uniformly random online player other than {@code self}, from any
     * world, or {@code null} when nobody else is online.
     */
    public static Player randomOther(Player self, Server server) {
        return randomOther(self, server, Set.of());
    }

    /**
     * A uniformly random online player other than {@code self} and not in
     * {@code exclude}, from any world, or {@code null} when nobody eligible is
     * online.
     */
    public static Player randomOther(Player self, Server server, Set<Player> exclude) {
        List<Player> others = new ArrayList<>();
        for (Player candidate : server.getOnlinePlayers()) {
            if (candidate != self && !exclude.contains(candidate)) {
                others.add(candidate);
            }
        }
        if (others.isEmpty()) {
            return null;
        }
        return others.get(ThreadLocalRandom.current().nextInt(others.size()));
    }
}
