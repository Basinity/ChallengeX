package com.basinity.challengex.fabric.lifecycle;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

/**
 * Puts every player into spectator mode when a run is lost, and puts them back
 * on reset or import. Each player's mode at the moment of loss is remembered,
 * so a creative host returns to creative rather than a forced default, and a
 * player who was already spectating is left alone both ways. The memory is
 * in-process only: after a server restart the restore is a no-op and the host
 * sets modes by hand.
 */
public final class LossSpectator {

    private final Map<UUID, GameType> previousModes = new HashMap<>();

    /** Switches every online player to spectator, remembering their mode. */
    public void apply(MinecraftServer server) {
        previousModes.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.gameMode() != GameType.SPECTATOR) {
                previousModes.put(player.getUUID(), player.gameMode());
                player.setGameMode(GameType.SPECTATOR);
            }
        }
    }

    /** Returns every remembered player still online to the mode they had at the loss. */
    public void restore(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            GameType mode = previousModes.remove(player.getUUID());
            if (mode != null) {
                player.setGameMode(mode);
            }
        }
        previousModes.clear();
    }
}
