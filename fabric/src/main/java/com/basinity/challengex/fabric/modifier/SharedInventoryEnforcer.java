package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * {@code modifier.share_inventory}: the players in the modifier's scope share
 * one inventory. Any change one makes — pickups, moving stacks, using or placing
 * items, armor, offhand — appears in every group member's inventory. The whole
 * addressable inventory is shared (hotbar, main, armor, offhand); each player
 * keeps their own selected hotbar slot, and the stack held on the cursor while a
 * screen is open stays private, because the cursor lives on the container menu
 * rather than in the inventory.
 *
 * <p>Sync is a per-tick diff rather than event hooks: nearly every inventory
 * mutation path would otherwise need its own hook, so instead each tick every
 * member's inventory is compared to the last synced state, and whoever changed
 * becomes the new shared state, pushed to the rest (up to a one-tick delay,
 * imperceptible in play). When the modifier first activates for a group the
 * first member seen seeds the shared inventory and the rest receive a copy,
 * which is also how a player joining an in-progress run is handed the group's
 * inventory.
 *
 * <p>The shared state is keyed per group: an {@code every_player} scope is one
 * group, and each distinct {@code specific_players} set is its own, so two
 * share-inventory modifiers with different rosters share independently. State is
 * dropped when a group empties and on server stop, so a fresh world never
 * inherits the previous one's shared inventory.
 */
public final class SharedInventoryEnforcer implements ModifierEnforcer {

    private static final String EVERY_PLAYER_GROUP = "*";

    /** Group key to its shared inventory (one item per addressable slot). */
    private final Map<String, List<ItemStack>> canonicalByGroup = new HashMap<>();
    /** Player to the shared inventory they were last synced to, for change detection. */
    private final Map<UUID, List<ItemStack>> lastSyncedByPlayer = new HashMap<>();
    /** Group key to its current member players, so a group's state is dropped once empty. */
    private final Map<String, Set<UUID>> membersByGroup = new HashMap<>();

    @Override
    public void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        String group = groupKey(modifier);
        membersByGroup.computeIfAbsent(group, ignored -> new HashSet<>()).add(player.getUUID());
        List<ItemStack> canonical = canonicalByGroup.get(group);
        if (canonical == null) {
            canonical = read(player);
            canonicalByGroup.put(group, canonical);
        } else {
            applyTo(player, canonical);
        }
        lastSyncedByPlayer.put(player.getUUID(), canonical);
    }

    @Override
    public void tick(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        if (!hasChanged(player, lastSyncedByPlayer.get(player.getUUID()))) {
            return;
        }
        String group = groupKey(modifier);
        List<ItemStack> canonical = read(player);
        canonicalByGroup.put(group, canonical);
        for (ServerPlayer member : groupMembers(modifier, server)) {
            if (!member.getUUID().equals(player.getUUID())) {
                applyTo(member, canonical);
            }
            lastSyncedByPlayer.put(member.getUUID(), canonical);
        }
    }

    @Override
    public void stop(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        lastSyncedByPlayer.remove(player.getUUID());
        String group = groupKey(modifier);
        Set<UUID> members = membersByGroup.get(group);
        if (members != null) {
            members.remove(player.getUUID());
            if (members.isEmpty()) {
                membersByGroup.remove(group);
                canonicalByGroup.remove(group);
            }
        }
    }

    @Override
    public void serverStopped() {
        canonicalByGroup.clear();
        lastSyncedByPlayer.clear();
        membersByGroup.clear();
    }

    /** Whether the player's live inventory differs from the shared state they last synced to. */
    private boolean hasChanged(ServerPlayer player, List<ItemStack> lastSynced) {
        Inventory inventory = player.getInventory();
        if (lastSynced == null || lastSynced.size() != inventory.getContainerSize()) {
            return true;
        }
        for (int slot = 0; slot < lastSynced.size(); slot++) {
            if (!ItemStack.matches(inventory.getItem(slot), lastSynced.get(slot))) {
                return true;
            }
        }
        return false;
    }

    /** A fresh copy of every addressable inventory slot, for the shared state. */
    private List<ItemStack> read(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        int size = inventory.getContainerSize();
        List<ItemStack> slots = new ArrayList<>(size);
        for (int slot = 0; slot < size; slot++) {
            slots.add(inventory.getItem(slot).copy());
        }
        return slots;
    }

    /** Overwrites the player's inventory with a copy of the shared state. */
    private void applyTo(ServerPlayer player, List<ItemStack> canonical) {
        Inventory inventory = player.getInventory();
        int size = Math.min(inventory.getContainerSize(), canonical.size());
        for (int slot = 0; slot < size; slot++) {
            inventory.setItem(slot, canonical.get(slot).copy());
        }
    }

    /** The online players sharing this modifier's inventory. */
    private List<ServerPlayer> groupMembers(Modifier modifier, MinecraftServer server) {
        Scope.Absolute scope = modifier.scope().orElseThrow();
        List<ServerPlayer> online = server.getPlayerList().getPlayers();
        if (scope instanceof Scope.SpecificPlayers specific) {
            List<ServerPlayer> members = new ArrayList<>();
            for (ServerPlayer player : online) {
                if (specific.playerIds().contains(player.getScoreboardName())) {
                    members.add(player);
                }
            }
            return members;
        }
        return online;
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
