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

/**
 * One number, shared by a group of players. Health, hunger and experience are
 * each a single value per player that the game already keeps in sync with the
 * client, so pooling them is the same job three times: notice what each member
 * changed, add it all up, and put the result back on everyone.
 *
 * <p>Sharing here is by mirroring rather than by reference, which is what
 * separates these from the shared inventory. An inventory is a list the game
 * reads through, so every member's can be pointed at one object; health is a
 * field the game writes to directly, so the only way to pool it is to watch for
 * changes and write the total back.
 *
 * <p>The whole group settles once per server tick, on whichever member happens
 * to be ticked first, rather than once per member. Settling per member would
 * lose changes: writing the pool to everybody as soon as the first member's
 * change was folded in would overwrite the second member's change before it had
 * been read, so two players hit in the same tick would only cost the pool one
 * hit. Every member's change since the last settle is gathered first, then the
 * result is written out.
 *
 * <p>The pool is capped by the smallest ceiling in the group, so a member with
 * a lower maximum keeps everyone honest rather than the group holding a value
 * one of them cannot display.
 *
 * <p>A dead member is out of the pool until they are back on their feet.
 * Writing to a player who is still on the death screen leaves the server
 * believing they are fine while their client says otherwise, which strands
 * them; and once a whole group is down the pool is forgotten, so the first of
 * them to respawn seeds a fresh one instead of being handed back the zero that
 * killed them.
 *
 * <p>Group state is keyed by scope exactly as the shared inventory's is: an
 * {@code every_player} scope is one group and each distinct set of named
 * players is its own, so two of these modifiers with different rosters pool
 * independently. A group's state is dropped once it empties and on server stop.
 */
abstract class SharedPoolEnforcer implements ModifierEnforcer {

    private static final String EVERY_PLAYER_GROUP = "*";

    private final Map<String, Double> poolByGroup = new HashMap<>();
    private final Map<String, Set<UUID>> membersByGroup = new HashMap<>();
    private final Map<String, Integer> settledAtTick = new HashMap<>();
    /** What each member's value was when the group last settled, to spot their change since. */
    private final Map<UUID, Double> lastSettled = new HashMap<>();

    /** The value being pooled, read off one player. */
    protected abstract double read(ServerPlayer player);

    /** Puts the pooled value back on one player. */
    protected abstract void write(ServerPlayer player, double value);

    /** The most this player can hold, or {@link Double#MAX_VALUE} when there is no cap. */
    protected abstract double ceiling(ServerPlayer player);

    /**
     * Whether several members gaining at once add up. Losses always do: two
     * players hit in the same tick both cost the pool, which is the whole point
     * of settling a group at once. Gains are the question, and the answer is
     * not the same for every pool. Food and experience are collected, so
     * collecting in two places should credit both. Health comes back on its own
     * for every member at the same time, and adding those up would heal a group
     * of two at twice the rate of a group of one, which is not what one shared
     * bar should do.
     */
    protected boolean gainsAddUp() {
        return true;
    }

    @Override
    public void start(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        String group = groupKey(modifier);
        membersByGroup.computeIfAbsent(group, ignored -> new HashSet<>()).add(player.getUUID());
        if (!player.isAlive()) {
            // Nothing is written to somebody who is down. The next settle after
            // they are back on their feet brings them into line.
            return;
        }
        if (poolByGroup.containsKey(group)) {
            // Joining an established group adopts its value, which is also how
            // somebody arriving partway through a run is brought into line.
            write(player, capped(poolByGroup.get(group), List.of(player)));
        } else {
            poolByGroup.put(group, read(player));
        }
        lastSettled.put(player.getUUID(), read(player));
    }

    @Override
    public void tick(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        String group = groupKey(modifier);
        Integer settled = settledAtTick.get(group);
        if (settled != null && settled == server.getTickCount()) {
            return;
        }
        settledAtTick.put(group, server.getTickCount());
        settle(group, server);
    }

    @Override
    public void stop(ServerPlayer player, Modifier modifier, MinecraftServer server) {
        lastSettled.remove(player.getUUID());
        String group = groupKey(modifier);
        Set<UUID> members = membersByGroup.get(group);
        if (members != null) {
            members.remove(player.getUUID());
            if (members.isEmpty()) {
                membersByGroup.remove(group);
                poolByGroup.remove(group);
                settledAtTick.remove(group);
            }
        }
    }

    @Override
    public void serverStopped() {
        poolByGroup.clear();
        membersByGroup.clear();
        settledAtTick.clear();
        lastSettled.clear();
    }

    private void settle(String group, MinecraftServer server) {
        List<ServerPlayer> members = livingMembers(group, server);
        if (members.isEmpty()) {
            poolByGroup.remove(group);
            return;
        }

        Double pooled = poolByGroup.get(group);
        double pool;
        if (pooled == null) {
            // Nobody was left a moment ago, so this member starts the pool over
            // rather than inheriting whatever emptied it.
            pool = read(members.getFirst());
        } else {
            pool = pooled;
            // Gather every change before writing any of it back, or the first
            // member's change would be written over the second member's before
            // it had been read.
            double losses = 0.0;
            double gains = 0.0;
            double largestGain = 0.0;
            for (ServerPlayer member : members) {
                Double before = lastSettled.get(member.getUUID());
                if (before == null) {
                    // Just arrived, or just back on their feet: they adopt the
                    // pool rather than bringing their own value into it.
                    continue;
                }
                double change = read(member) - before;
                if (change < 0) {
                    losses += change;
                } else {
                    gains += change;
                    largestGain = Math.max(largestGain, change);
                }
            }
            pool += losses + (gainsAddUp() ? gains : largestGain);
        }

        double value = capped(pool, members);
        for (ServerPlayer member : members) {
            write(member, value);
            // Read back rather than trusting the write: the game may clamp it,
            // and a difference we did not notice would read as a change next tick.
            lastSettled.put(member.getUUID(), read(member));
        }
        poolByGroup.put(group, value);
    }

    /** The pool held within zero and the smallest ceiling among these players. */
    private double capped(double pool, List<ServerPlayer> members) {
        double ceiling = Double.MAX_VALUE;
        for (ServerPlayer member : members) {
            ceiling = Math.min(ceiling, ceiling(member));
        }
        return Math.max(0.0, Math.min(pool, ceiling));
    }

    /**
     * The members who are online and alive. A dead one is dropped from the
     * record as well as skipped, so that when they respawn they read as having
     * just arrived and adopt the pool instead of contributing the whole of
     * their restored value to it.
     */
    private List<ServerPlayer> livingMembers(String group, MinecraftServer server) {
        List<ServerPlayer> members = new ArrayList<>();
        for (UUID id : membersByGroup.getOrDefault(group, Set.of())) {
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && player.isAlive()) {
                members.add(player);
            } else {
                lastSettled.remove(id);
            }
        }
        return members;
    }

    /** A stable key for the group a scope defines: one for every-player, one per named roster. */
    private String groupKey(Modifier modifier) {
        Scope.Absolute scope = modifier.scope().orElseThrow();
        if (scope instanceof Scope.SpecificPlayers specific) {
            return String.join(",", new TreeSet<>(specific.playerIds()));
        }
        return EVERY_PLAYER_GROUP;
    }
}
