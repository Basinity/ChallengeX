package com.basinity.challengex.fabric.effect;

import java.util.ArrayList;
import java.util.List;
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
        List<ServerPlayer> others = new ArrayList<>();
        for (ServerPlayer candidate : server.getPlayerList().getPlayers()) {
            if (candidate != self) {
                others.add(candidate);
            }
        }
        if (others.isEmpty()) {
            return null;
        }
        return others.get(self.getRandom().nextInt(others.size()));
    }
}
