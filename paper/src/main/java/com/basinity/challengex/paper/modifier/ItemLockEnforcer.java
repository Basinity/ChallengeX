package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.common.modifier.SharedPool;
import com.basinity.challengex.core.model.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * {@code modifier.item_lock}: an item one player carries is theirs alone, and
 * nobody else in the group can hold it. The optional {@code item} parameter
 * narrows the rule to one item; omitting it locks every item there is, the same
 * shape {@code disable_item_use} has.
 *
 * <p>Holding is what grants a lock and losing possession is what releases it, so
 * every way an item can leave an inventory releases it without being handled
 * separately: dropped, eaten, burned as fuel, stored in a chest, lost on death,
 * or the holder disconnecting. Only loose stacks in a player's own inventory
 * count, which is the hotbar, the main grid, the armour slots and the offhand.
 *
 * <p>A settle reads who is carrying what, releases the locks on items nobody
 * carries any more, and resolves each remaining item to one holder: whoever
 * already held it keeps it, and if nobody did, one carrier is chosen at random.
 * Everybody else carrying that item has it dropped at their feet. Run start
 * needs no special case, because two players beginning with the same item is
 * the same conflict as any other.
 *
 * <p>The ejection is the backstop rather than the normal experience:
 * {@link ItemLockModifierSource} refuses the common ways of acquiring a locked
 * item first, so a player usually never takes it in the first place. What those
 * refusals do not cover, an item handed over by a give-item effect among them,
 * ends here instead.
 */
public final class ItemLockEnforcer implements ModifierEnforcer {

    private static final String EVERY_ITEM = "*";

    private final Map<String, Set<UUID>> membersByGroup = new HashMap<>();
    /** Who holds each locked item id, per group. */
    private final Map<String, Map<String, UUID>> holdersByGroup = new HashMap<>();
    private final Map<String, Integer> settledAtTick = new HashMap<>();
    private final Map<String, Integer> joinedAtTick = new HashMap<>();

    @Override
    public void start(Player player, Modifier modifier, Server server) {
        String group = SharedPool.groupKey(modifier);
        membersByGroup.computeIfAbsent(group, ignored -> new HashSet<>()).add(player.getUniqueId());
        joinedAtTick.put(group, server.getCurrentTick());
    }

    @Override
    public void tick(Player player, Modifier modifier, Server server) {
        String group = SharedPool.groupKey(modifier);
        int tick = server.getCurrentTick();
        // The enforcement pass adds members one player at a time, so settling on
        // a tick somebody joined would judge the group on however much of it had
        // arrived and hand every lock to whoever was ticked first.
        if (Integer.valueOf(tick).equals(joinedAtTick.get(group))) {
            return;
        }
        Integer settled = settledAtTick.get(group);
        if (settled != null && settled == tick) {
            return;
        }
        settledAtTick.put(group, tick);
        settle(group, modifier, server, tick);
    }

    @Override
    public void stop(Player player, Modifier modifier, Server server) {
        String group = SharedPool.groupKey(modifier);
        ItemLocks.clear(player.getUniqueId());
        Set<UUID> members = membersByGroup.get(group);
        if (members == null) {
            return;
        }
        members.remove(player.getUniqueId());
        // A departing member's locks go with them, or the group would keep
        // refusing items nobody is holding any more.
        holdersByGroup.getOrDefault(group, Map.of())
                .values().removeIf(holder -> holder.equals(player.getUniqueId()));
        if (members.isEmpty()) {
            membersByGroup.remove(group);
            holdersByGroup.remove(group);
            settledAtTick.remove(group);
            joinedAtTick.remove(group);
        }
    }

    @Override
    public void disabled() {
        membersByGroup.clear();
        holdersByGroup.clear();
        settledAtTick.clear();
        joinedAtTick.clear();
        ItemLocks.clearAll();
    }

    private void settle(String group, Modifier modifier, Server server, int tick) {
        String only = ModifierParams.string(modifier, "item");
        List<Player> members = onlineMembers(group, server);
        if (members.isEmpty()) {
            holdersByGroup.remove(group);
            return;
        }

        // Who is carrying what, this tick.
        Map<String, Set<UUID>> carriers = new HashMap<>();
        for (Player member : members) {
            for (String itemId : carried(member, only)) {
                carriers.computeIfAbsent(itemId, ignored -> new LinkedHashSet<>())
                        .add(member.getUniqueId());
            }
        }

        Map<String, UUID> holders = holdersByGroup.computeIfAbsent(group, ignored -> new HashMap<>());
        // Nobody is carrying it any more, so the lock is released.
        holders.keySet().retainAll(carriers.keySet());

        for (Map.Entry<String, Set<UUID>> entry : carriers.entrySet()) {
            String itemId = entry.getKey();
            Set<UUID> carrying = entry.getValue();
            UUID holder = holders.get(itemId);
            if (holder == null || !carrying.contains(holder)) {
                holder = pick(carrying);
                holders.put(itemId, holder);
            }
            for (Player member : members) {
                if (!member.getUniqueId().equals(holder) && carrying.contains(member.getUniqueId())) {
                    eject(member, itemId);
                }
            }
        }

        publish(members, holders, tick);
    }

    /** Tells each member which ids somebody else is holding, for the refusals to read. */
    private static void publish(List<Player> members, Map<String, UUID> holders, int tick) {
        for (Player member : members) {
            Set<String> blocked = new HashSet<>();
            for (Map.Entry<String, UUID> held : holders.entrySet()) {
                if (!held.getValue().equals(member.getUniqueId())) {
                    blocked.add(held.getKey());
                }
            }
            ItemLocks.publish(member.getUniqueId(), tick, blocked);
        }
    }

    /** The distinct item ids loose in this player's own inventory. */
    private static Set<String> carried(Player player, String only) {
        Set<String> ids = new LinkedHashSet<>();
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String id = ItemLocks.idOf(stack);
            if (only == null || only.equals(EVERY_ITEM) || only.equals(id)) {
                ids.add(id);
            }
        }
        return ids;
    }

    /** Drops every stack of this item at the player's feet. */
    private static void eject(Player player, String itemId) {
        PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack == null || stack.isEmpty() || !ItemLocks.idOf(stack).equals(itemId)) {
                continue;
            }
            inventory.setItem(slot, null);
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
    }

    private static UUID pick(Set<UUID> carrying) {
        List<UUID> candidates = new ArrayList<>(carrying);
        return candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
    }

    private List<Player> onlineMembers(String group, Server server) {
        List<Player> online = new ArrayList<>();
        for (UUID id : membersByGroup.getOrDefault(group, Set.of())) {
            Player player = server.getPlayer(id);
            if (player != null && player.isOnline()) {
                online.add(player);
            }
        }
        return online;
    }
}
