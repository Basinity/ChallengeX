package com.basinity.challengex.fabric.modifier;

import com.basinity.challengex.common.modifier.ModifierParams;
import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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
 * The contents of a bundle do not, so stashing an item inside one gives up the
 * lock, and neither does a stack sitting on the cursor mid-click.
 *
 * <p>The whole group settles once a server tick, on whichever member is ticked
 * first, exactly as the shared pools do, except on a tick where somebody joined
 * the group: the enforcement pass adds members one player at a time, so settling
 * then would judge the group on however much of it had arrived and hand every
 * lock to the player ticked first. A settle reads who is carrying what,
 * releases the locks on items nobody carries any more, and resolves each
 * remaining item to one holder: whoever already held it keeps it, and if nobody
 * did, one carrier is chosen at random. Everybody else carrying that item has it
 * dropped at their feet. Run start needs no special case, because two players
 * beginning with the same item is the same conflict as any other; nor does a
 * player joining partway through, an import mid-run, or an item arriving by a
 * path the refusals below do not cover.
 *
 * <p>Refusals are what keep that ejection from being the normal experience.
 * {@link ItemLocks} publishes what each member may not take, and three Mixins
 * read it: taking a locked item into the inventory fails, so it stays on the
 * ground or wherever it was; a container slot holding one cannot be picked up,
 * which covers chests, crafting and furnace output, and trades; and pulling one
 * out of a bundle does nothing. What is left over, an anvil or smithing result
 * (those slots decide pickup for themselves) or an item handed over by a
 * give-item effect, ends in the ejection instead.
 *
 * <p>Group state is keyed by scope the way the shared pools and the shared
 * inventory key theirs: every-player is one group, and each distinct set of named
 * players is its own, so two of these locking different rosters never see each
 * other's items.
 */
public final class ItemLockEnforcer implements ModifierEnforcer {

    private static final String EVERY_PLAYER_GROUP = "*";
    private static final String EVERY_ITEM = "*";

    private final Map<String, Set<UUID>> membersByGroup = new HashMap<>();
    /** Who holds each locked item id, per group. */
    private final Map<String, Map<String, UUID>> holdersByGroup = new HashMap<>();
    private final Map<String, Integer> settledAtTick = new HashMap<>();
    private final Map<String, Integer> joinedAtTick = new HashMap<>();

    @Override
    public void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        String group = groupKey(modifier);
        membersByGroup.computeIfAbsent(group, ignored -> new HashSet<>()).add(player.getUUID());
        joinedAtTick.put(group, server.getTickCount());
    }

    @Override
    public void tick(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        String group = groupKey(modifier);
        if (Integer.valueOf(server.getTickCount()).equals(joinedAtTick.get(group))) {
            // Somebody joined the group this tick, and the enforcement pass adds
            // members one player at a time. Settling now would judge the group on
            // whoever happens to be in it so far, handing every lock to the player
            // ticked first; the rest of the group is in it by the next tick.
            return;
        }
        Integer settled = settledAtTick.get(group);
        if (settled != null && settled == server.getTickCount()) {
            return;
        }
        settledAtTick.put(group, server.getTickCount());
        settle(group, modifier, server);
    }

    @Override
    public void stop(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        ItemLocks.clear(player.getUUID());
        String group = groupKey(modifier);
        Set<UUID> members = membersByGroup.get(group);
        if (members != null) {
            members.remove(player.getUUID());
            if (members.isEmpty()) {
                membersByGroup.remove(group);
                holdersByGroup.remove(group);
                settledAtTick.remove(group);
                joinedAtTick.remove(group);
            }
        }
    }

    @Override
    public void serverStopped() {
        membersByGroup.clear();
        holdersByGroup.clear();
        settledAtTick.clear();
        joinedAtTick.clear();
        ItemLocks.clearAll();
    }

    private void settle(String group, Modifier modifier, MinecraftServer server) {
        Map<String, UUID> holders = holdersByGroup.computeIfAbsent(group, ignored -> new HashMap<>());
        List<ServerPlayer> members = onlineMembers(group, server);
        if (members.isEmpty()) {
            holders.clear();
            return;
        }

        String onlyItem = ModifierParams.string(modifier, "item");
        Map<String, List<ServerPlayer>> carriers = new HashMap<>();
        for (ServerPlayer member : members) {
            for (String itemId : carriedIds(member, onlyItem)) {
                carriers.computeIfAbsent(itemId, ignored -> new ArrayList<>()).add(member);
            }
        }
        // An item nobody in the group is carrying is nobody's any more.
        holders.keySet().retainAll(carriers.keySet());

        RandomSource random = members.getFirst().level().getRandom();
        for (Map.Entry<String, List<ServerPlayer>> carried : carriers.entrySet()) {
            List<ServerPlayer> carrying = carried.getValue();
            ServerPlayer keeper = keeperOf(carrying, holders.get(carried.getKey()), random);
            holders.put(carried.getKey(), keeper.getUUID());
            for (ServerPlayer other : carrying) {
                if (other != keeper) {
                    dropAll(other, carried.getKey());
                }
            }
        }

        publish(members, holders, server.getTickCount());
    }

    /** The player who keeps this item: the one who already held it, else one at random. */
    private ServerPlayer keeperOf(List<ServerPlayer> carrying, UUID holder, RandomSource random) {
        for (ServerPlayer candidate : carrying) {
            if (candidate.getUUID().equals(holder)) {
                return candidate;
            }
        }
        return carrying.get(random.nextInt(carrying.size()));
    }

    /** The distinct ids this player is carrying, narrowed to the locked item when there is one. */
    private Set<String> carriedIds(ServerPlayer player, String onlyItem) {
        Inventory inventory = player.getInventory();
        Set<String> ids = new LinkedHashSet<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            String itemId = ItemLocks.idOf(stack);
            if (onlyItem == null || onlyItem.equals(itemId)) {
                ids.add(itemId);
            }
        }
        return ids;
    }

    /**
     * Drops every stack of one item out of a player's inventory at their feet.
     * The menu the player has open broadcasts the emptied slots on its own next
     * tick, the same way any other server-side inventory change reaches a client.
     */
    private void dropAll(ServerPlayer player, String itemId) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && ItemLocks.idOf(stack).equals(itemId)) {
                inventory.setItem(slot, ItemStack.EMPTY);
                player.drop(stack, false, Prediction.SERVER_ONLY);
            }
        }
    }

    private void publish(List<ServerPlayer> members, Map<String, UUID> holders, int tick) {
        for (ServerPlayer member : members) {
            Set<String> blocked = new HashSet<>();
            for (Map.Entry<String, UUID> lock : holders.entrySet()) {
                if (!lock.getValue().equals(member.getUUID())) {
                    blocked.add(lock.getKey());
                }
            }
            ItemLocks.publish(member.getUUID(), tick, blocked);
        }
    }

    /** The members who are online. An offline one holds nothing, so their locks lapse. */
    private List<ServerPlayer> onlineMembers(String group, MinecraftServer server) {
        List<ServerPlayer> members = new ArrayList<>();
        for (UUID id : membersByGroup.getOrDefault(group, Set.of())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) {
                members.add(player);
            } else {
                ItemLocks.clear(id);
            }
        }
        return members;
    }

    /**
     * A stable key for the group a modifier defines: one for every-player, one
     * per named roster, and a separate one per locked item, since a lock on one
     * item and a lock on everything are two different competitions even when the
     * same players are in both.
     */
    private String groupKey(Modifier modifier) {
        Scope.Absolute scope = modifier.scope().orElseThrow();
        String roster = scope instanceof Scope.SpecificPlayers specific
                ? String.join(",", new TreeSet<>(specific.playerIds()))
                : EVERY_PLAYER_GROUP;
        String onlyItem = ModifierParams.string(modifier, "item");
        return roster + "|" + (onlyItem == null ? EVERY_ITEM : onlyItem);
    }
}
