package com.basinity.challengex.paper.modifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.inventory.ItemStack;

/**
 * What {@code modifier.item_lock} currently forbids each player, published by
 * {@link ItemLockEnforcer} once a tick and read by the sources that refuse an
 * acquisition. Each player gets the flat set of item ids somebody else holds, so
 * refusing a click is one map lookup, with no scope to resolve and no parameters
 * to read at the point of the click.
 *
 * <p>Publishing is tick-aware so that two item locks covering one player (two
 * copies of the modifier, or an every-player one alongside a named one) add up
 * rather than the group settled last in the tick deciding alone.
 */
final class ItemLocks {

    private record Blocked(int tick, Set<String> itemIds) {
    }

    private static final Map<UUID, Blocked> blockedByPlayer = new ConcurrentHashMap<>();

    private ItemLocks() {
    }

    /** Whether this stack is held by somebody else, and so cannot come into this player's hands. */
    static boolean blocked(UUID playerId, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Blocked blocked = blockedByPlayer.get(playerId);
        return blocked != null && blocked.itemIds().contains(idOf(stack));
    }

    static void publish(UUID playerId, int tick, Set<String> itemIds) {
        Blocked previous = blockedByPlayer.get(playerId);
        Set<String> combined = new HashSet<>(itemIds);
        if (previous != null && previous.tick() == tick) {
            combined.addAll(previous.itemIds());
        }
        if (combined.isEmpty()) {
            blockedByPlayer.remove(playerId);
        } else {
            blockedByPlayer.put(playerId, new Blocked(tick, Set.copyOf(combined)));
        }
    }

    static void clear(UUID playerId) {
        blockedByPlayer.remove(playerId);
    }

    static void clearAll() {
        blockedByPlayer.clear();
    }

    static String idOf(ItemStack stack) {
        return stack.getType().getKey().toString();
    }
}
