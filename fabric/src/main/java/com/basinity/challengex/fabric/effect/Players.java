package com.basinity.challengex.fabric.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Player-selection helpers shared by effect handlers. */
public final class Players {

    private Players() {
    }

    /**
     * A uniformly random online player other than {@code self}, from any
     * dimension, or {@code null} when nobody else is online.
     */
    public static ServerPlayer randomOther(ServerPlayer self, MinecraftServer server) {
        return randomOther(self, server, Set.of());
    }

    /**
     * A uniformly random online player other than {@code self} and not in
     * {@code exclude}, from any dimension, or {@code null} when nobody
     * eligible is online.
     */
    public static ServerPlayer randomOther(ServerPlayer self, MinecraftServer server, Set<ServerPlayer> exclude) {
        List<ServerPlayer> others = new ArrayList<>();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate != self && !exclude.contains(candidate)) {
                others.add(candidate);
            }
        }
        if (others.isEmpty()) {
            return null;
        }
        return others.get(self.getRandom().nextInt(others.size()));
    }
}
