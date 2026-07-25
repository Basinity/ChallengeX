package com.basinity.challengex.fabric.modifier;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * What {@code modifier.item_lock} currently forbids each player, published by
 * {@link ItemLockEnforcer} once a tick and read by the Mixins that refuse an
 * acquisition. Each player gets the flat set of item ids somebody else holds, so
 * refusing a click is one map lookup, with no scope to resolve and no parameters
 * to read at the point of the click.
 *
 * <p>Players are keyed by UUID rather than by scoreboard name because the
 * singleplayer client shares this JVM with its server: reading the same set the
 * server wrote lets the client predict a refusal instead of showing the item
 * moving and then being corrected. A player under no item lock is simply absent.
 *
 * <p>Publishing is tick-aware so that two item locks covering one player (two
 * copies of the modifier, or an every-player one alongside a named one) add up
 * rather than the group settled last in the tick deciding alone.
 */
public final class ItemLocks {

    private record Blocked(int tick, Set<String> itemIds) {
    }

    private static final Map<UUID, Blocked> blockedByPlayer = new ConcurrentHashMap<>();

    private ItemLocks() {
    }

    /** Whether this stack is held by somebody else, and so cannot come into this player's hands. */
    public static boolean blocked(Player player, ItemStack stack) {
        return blocked(player.getUUID(), stack);
    }

    /** The same question where only the player's id is at hand. */
    public static boolean blocked(UUID playerId, ItemStack stack) {
        if (stack.isEmpty()) {
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
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
