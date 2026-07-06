package com.basinity.challengex.fabric.trigger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Base for the triggers with no game event behind them, detected instead by
 * reading one value off every player once a server tick and comparing it with
 * the tick before. Sneaking, height, health, hunger, and the current biome are
 * all state a player is in rather than something the game announces.
 *
 * <p>The first tick a player is seen records a baseline and emits nothing, so
 * joining a server is never mistaken for a change. Per-player state is dropped
 * when a player goes offline and when the server stops, so a fresh world starts
 * from a fresh baseline rather than the last one's values.
 */
public abstract class PlayerPollTriggerSource<T> implements TriggerSource {

    private final Map<UUID, T> lastSeen = new HashMap<>();

    @Override
    public final void register(TriggerContext context) {
        ServerTickEvents.END_SERVER_TICK.register(server -> poll(server, context));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> lastSeen.clear());
    }

    private void poll(MinecraftServer server, TriggerContext context) {
        Set<UUID> online = new HashSet<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            online.add(player.getUUID());
            T current = read(player);
            T previous = lastSeen.put(player.getUUID(), current);
            if (previous != null && !previous.equals(current)) {
                onChange(player, previous, current, context);
            }
        }
        lastSeen.keySet().retainAll(online);
    }

    /** The value to watch, read fresh this tick. */
    protected abstract T read(ServerPlayer player);

    /** Called only when this tick's value differs from the previous tick's. */
    protected abstract void onChange(ServerPlayer player, T previous, T current, TriggerContext context);
}
