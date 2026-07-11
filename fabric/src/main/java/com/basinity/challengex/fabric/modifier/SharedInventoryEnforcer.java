package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Scope;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code modifier.share_inventory}: the players in the modifier's scope share
 * one inventory. Any change one makes — pickups, moving stacks, using or placing
 * items, armor, offhand — appears in every group member's inventory. The whole
 * addressable inventory is shared (hotbar, main, armor, offhand); each player
 * keeps their own selected hotbar slot, and the stack held on the cursor while a
 * screen is open stays private, because the cursor lives on the container menu
 * rather than in the inventory.
 *
 * <p>Sharing is by reference, not by copying: every group member's {@code
 * Inventory.items} list and their equipment's armor/offhand map point at one
 * {@link SharedInventory} object per group (through {@link
 * SharedInventoryAccess}, a Mixin seam on {@code Inventory}). A mutation is
 * therefore inherently visible to everyone with no per-tick diffing and no
 * same-tick loss when two members change the inventory in the same tick. {@link
 * #tick} does nothing; the wiring lives in {@link #start}/{@link #stop} and, for
 * the player objects vanilla rebuilds, in the respawn and reconnect events
 * hooked by {@link #register}.
 *
 * <p>When the modifier first activates for a group the first member seen seeds
 * the shared inventory and later members receive it, which is also how a player
 * joining an in-progress run is handed the group's inventory. Leaving the group
 * ({@link #stop}) detaches with a private copy, so the departing player keeps
 * the items and the shared object lives on for the rest.
 *
 * <p>The shared state is keyed per group: an {@code every_player} scope is one
 * group, and each distinct {@code specific_players} set is its own, so two
 * share-inventory modifiers with different rosters share independently. State is
 * dropped when a group empties and on server stop, so a fresh world never
 * inherits the previous one's shared inventory.
 */
public final class SharedInventoryEnforcer implements ModifierEnforcer {

    private static final String EVERY_PLAYER_GROUP = "*";

    /** Group key to its shared inventory object every member points at. */
    private final Map<String, SharedInventory> sharedByGroup = new HashMap<>();
    /** Group key to its current member players, so a group's state is dropped once empty. */
    private final Map<String, Set<UUID>> membersByGroup = new HashMap<>();
    /**
     * Player to the group they belong to, kept across disconnect and respawn so
     * the respawn and reconnect events can re-point a rebuilt inventory without
     * the enforcer's start/stop having to fire again.
     */
    private final Map<UUID, String> groupByPlayer = new HashMap<>();

    @Override
    public void register() {
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> rewire(newPlayer));
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> rewire(handler.player));
    }

    @Override
    public void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        String group = groupKey(modifier);
        membersByGroup.computeIfAbsent(group, ignored -> new HashSet<>()).add(player.getUUID());
        groupByPlayer.put(player.getUUID(), group);
        SharedInventoryAccess inventory = (SharedInventoryAccess) player.getInventory();
        SharedInventory shared = sharedByGroup.get(group);
        if (shared == null) {
            shared = new SharedInventory();
            inventory.challengex$seedInto(shared);
            sharedByGroup.put(group, shared);
        }
        inventory.challengex$share(shared);
    }

    @Override
    public void stop(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        ((SharedInventoryAccess) player.getInventory()).challengex$detach();
        groupByPlayer.remove(player.getUUID());
        String group = groupKey(modifier);
        Set<UUID> members = membersByGroup.get(group);
        if (members != null) {
            members.remove(player.getUUID());
            if (members.isEmpty()) {
                membersByGroup.remove(group);
                sharedByGroup.remove(group);
            }
        }
    }

    @Override
    public void serverStopped() {
        sharedByGroup.clear();
        membersByGroup.clear();
        groupByPlayer.clear();
    }

    /** Re-points a rebuilt player inventory at its group's shared object, if it has one. */
    private void rewire(ServerPlayer player) {
        String group = groupByPlayer.get(player.getUUID());
        if (group == null) {
            return;
        }
        SharedInventory shared = sharedByGroup.get(group);
        if (shared != null) {
            ((SharedInventoryAccess) player.getInventory()).challengex$share(shared);
        }
    }

    /** A stable key for the group a scope defines: one for every-player, one per specific roster. */
    private String groupKey(Modifier modifier) {
        Scope.Absolute scope = modifier.scope().orElseThrow();
        if (scope instanceof Scope.SpecificPlayers specific) {
            return String.join(",", new TreeSet<>(specific.playerIds()));
        }
        return EVERY_PLAYER_GROUP;
    }
}
