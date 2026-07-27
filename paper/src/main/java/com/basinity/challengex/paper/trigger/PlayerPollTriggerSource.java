package com.basinity.challengex.paper.trigger;

import com.basinity.challengex.common.trigger.TriggerContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Base for the triggers with no game event behind them, detected instead by
 * reading one value off every player once a tick and comparing it with the tick
 * before. Sneaking, height, health, hunger, and the current biome are all state
 * a player is in rather than something the game announces.
 *
 * <p>The first tick a player is seen records a baseline and emits nothing, so
 * joining a server is never mistaken for a change. Per-player state is dropped
 * when a player goes offline, and the whole map dies with the plugin instance,
 * so a reload starts from a fresh baseline.
 */
public abstract class PlayerPollTriggerSource<T> implements TriggerSource {

    private final Map<UUID, T> lastSeen = new HashMap<>();

    @Override
    public final void register(TriggerContext context, Plugin plugin) {
        Server server = plugin.getServer();
        server.getScheduler().runTaskTimer(plugin, () -> poll(server, context), 1L, 1L);
    }

    private void poll(Server server, TriggerContext context) {
        Set<UUID> online = new HashSet<>();
        for (Player player : server.getOnlinePlayers()) {
            online.add(player.getUniqueId());
            T current = read(player);
            T previous = lastSeen.put(player.getUniqueId(), current);
            if (previous != null && !previous.equals(current)) {
                onChange(player, previous, current, context);
            }
        }
        lastSeen.keySet().retainAll(online);
    }

    /** The value to watch, read fresh this tick. */
    protected abstract T read(Player player);

    /** Called only when this tick's value differs from the previous tick's. */
    protected abstract void onChange(Player player, T previous, T current, TriggerContext context);
}
