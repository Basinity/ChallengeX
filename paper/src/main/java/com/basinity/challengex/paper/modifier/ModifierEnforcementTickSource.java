package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierContext;
import com.basinity.challengex.core.model.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Drives every {@link ModifierEnforcer} once a tick, diffing each online
 * player's currently active modifiers against what was active for them last
 * tick to fire {@link ModifierEnforcer#start}/{@link ModifierEnforcer#stop} on
 * the transitions and {@link ModifierEnforcer#tick} every tick in between.
 *
 * <p>Per-player state is kept rather than cleared on disconnect: a modifier
 * that stops applying while its player is offline (the run ends or resets,
 * leaving an attribute modifier in place or an infinite-duration effect never
 * removed) still needs its {@code stop} to fire once, which happens the next
 * tick the player is seen, whether that is this tick or one after they
 * reconnect.
 */
public final class ModifierEnforcementTickSource {

    private final Map<UUID, Map<String, Modifier>> activeByPlayer = new HashMap<>();
    private final Map<String, ModifierEnforcer> enforcers = ModifierEnforcers.byId();

    public void register(ModifierContext context, Plugin plugin) {
        Server server = plugin.getServer();
        server.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : server.getOnlinePlayers()) {
                tickPlayer(player, context, server);
            }
        }, 1L, 1L);
    }

    /** Drops all state, for a plugin disable. */
    public void disabled() {
        activeByPlayer.clear();
        enforcers.values().forEach(ModifierEnforcer::disabled);
    }

    private void tickPlayer(Player player, ModifierContext context, Server server) {
        Map<String, Modifier> current = new HashMap<>();
        for (Modifier modifier : context.activeModifiersFor(player.getName())) {
            current.putIfAbsent(modifier.modifierId(), modifier);
        }
        Map<String, Modifier> previous = activeByPlayer.getOrDefault(player.getUniqueId(), Map.of());
        for (Map.Entry<String, Modifier> entry : current.entrySet()) {
            ModifierEnforcer enforcer = enforcers.get(entry.getKey());
            if (enforcer == null) {
                continue;
            }
            if (!previous.containsKey(entry.getKey())) {
                enforcer.start(player, entry.getValue(), server);
            }
            enforcer.tick(player, entry.getValue(), server);
        }
        for (Map.Entry<String, Modifier> entry : previous.entrySet()) {
            if (!current.containsKey(entry.getKey())) {
                ModifierEnforcer enforcer = enforcers.get(entry.getKey());
                if (enforcer != null) {
                    enforcer.stop(player, entry.getValue(), server);
                }
            }
        }
        activeByPlayer.put(player.getUniqueId(), current);
    }
}
