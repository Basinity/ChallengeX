package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Drives every {@link ModifierEnforcer} once a server tick, diffing each
 * online player's currently active modifiers against what was active for them
 * last tick to fire {@link ModifierEnforcer#start}/{@link
 * ModifierEnforcer#stop} on the transitions and {@link ModifierEnforcer#tick}
 * every tick in between.
 *
 * <p>Per-player state is kept rather than cleared on disconnect: a modifier
 * whose expiry lapses while its player is offline (an attribute modifier left
 * in place, an infinite-duration effect never removed) still needs its {@code
 * stop} to fire once, which happens the next tick the player is seen, whether
 * that is this tick or one after they reconnect. Only a server stop clears it,
 * since a fresh world starts every enforcer from a clean slate.
 */
public final class ModifierEnforcementTickSource {

    private final Map<UUID, Map<String, Modifier>> activeByPlayer = new HashMap<>();
    private final Map<String, ModifierEnforcer> enforcers = ModifierEnforcers.byId();

    public void register(ModifierContext context) {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick(server, context));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            activeByPlayer.clear();
            enforcers.values().forEach(ModifierEnforcer::serverStopped);
        });
    }

    private void tick(MinecraftServer server, ModifierContext context) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            tickPlayer(player, context, server);
        }
    }

    private void tickPlayer(ServerPlayer player, ModifierContext context, MinecraftServer server) {
        Map<String, Modifier> current = new HashMap<>();
        for (Modifier modifier : context.activeModifiersFor(player.getScoreboardName())) {
            current.putIfAbsent(modifier.modifierId(), modifier);
        }
        Map<String, Modifier> previous = activeByPlayer.getOrDefault(player.getUUID(), Map.of());
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
        activeByPlayer.put(player.getUUID(), current);
    }
}
