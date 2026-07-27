package com.basinity.challengex.common.modifier;

import com.basinity.challengex.core.model.Modifier;
import com.basinity.challengex.core.model.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

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
 * <p>The whole group settles once per tick, on whichever member happens to be
 * ticked first, rather than once per member. Settling per member would lose
 * changes: writing the pool to everybody as soon as the first member's change
 * was folded in would overwrite the second member's change before it had been
 * read, so two players hit in the same tick would only cost the pool one hit.
 * Every member's change since the last settle is gathered first, then the
 * result is written out.
 *
 * <p>All of the arithmetic and bookkeeping lives here rather than in an
 * adapter, because none of it is about a platform: reading and writing the
 * value on a real player is the only part that is.
 */
public final class SharedPool {

    private static final String EVERY_PLAYER_GROUP = "*";

    /** One member's state as the adapter currently sees it. */
    public record Member(UUID id, double value, double ceiling) {
    }

    private final boolean gainsAddUp;
    private final Map<String, Double> poolByGroup = new HashMap<>();
    private final Map<String, Set<UUID>> membersByGroup = new HashMap<>();
    private final Map<String, Integer> settledAtTick = new HashMap<>();
    /** What each member's value was when the group last settled, to spot their change since. */
    private final Map<UUID, Double> lastSettled = new HashMap<>();

    /**
     * @param gainsAddUp whether several members gaining at once add up. Losses
     *     always do: two players hit in the same tick both cost the pool, which
     *     is the whole point of settling a group at once. Gains are the
     *     question, and the answer is not the same for every pool. Food and
     *     experience are collected, so collecting in two places should credit
     *     both. Health comes back on its own for every member at the same time,
     *     and adding those up would heal a group of two at twice the rate of a
     *     group of one, which is not what one shared bar should do.
     */
    public SharedPool(boolean gainsAddUp) {
        this.gainsAddUp = gainsAddUp;
    }

    /** A stable key for the group a scope defines: one for every-player, one per named roster. */
    public static String groupKey(Modifier modifier) {
        Scope.Absolute scope = modifier.scope().orElseThrow();
        if (scope instanceof Scope.SpecificPlayers specific) {
            return String.join(",", new TreeSet<>(specific.playerIds()));
        }
        return EVERY_PLAYER_GROUP;
    }

    /**
     * Brings a player into a group. Joining and taking on a value are separate
     * steps because somebody who is down joins the group without contributing
     * to or adopting the pool: they are brought into line by the first settle
     * after they are back on their feet.
     */
    public void addMember(String group, UUID id) {
        membersByGroup.computeIfAbsent(group, ignored -> new HashSet<>()).add(id);
    }

    /** Whether this group already has a value, as opposed to needing one seeded. */
    public boolean hasPool(String group) {
        return poolByGroup.containsKey(group);
    }

    /**
     * The value a player joining an established group takes on, held to their
     * own ceiling. This is also how somebody arriving partway through a run is
     * brought into line.
     */
    public double adopt(String group, double ceiling) {
        return Math.max(0.0, Math.min(poolByGroup.get(group), ceiling));
    }

    /** Starts a group's pool off at one member's value. */
    public void seed(String group, double value) {
        poolByGroup.put(group, value);
    }

    /** Records what a player actually holds after a write, since the game may clamp it. */
    public void recordSettled(UUID id, double actual) {
        lastSettled.put(id, actual);
    }

    /** Drops a player from a group, forgetting the group once it empties. */
    public void leave(String group, UUID id) {
        lastSettled.remove(id);
        Set<UUID> members = membersByGroup.get(group);
        if (members == null) {
            return;
        }
        members.remove(id);
        if (members.isEmpty()) {
            membersByGroup.remove(group);
            poolByGroup.remove(group);
            settledAtTick.remove(group);
        }
    }

    /** Forgets a member's last-settled value, for one who is down or gone. */
    public void forget(UUID id) {
        lastSettled.remove(id);
    }

    /** The ids currently in a group, for the adapter to resolve to real players. */
    public Set<UUID> members(String group) {
        return Set.copyOf(membersByGroup.getOrDefault(group, Set.of()));
    }

    /**
     * Whether this group still needs settling this tick. True once per tick per
     * group, so the first member ticked settles it and the rest fall through.
     */
    public boolean claimTick(String group, int tick) {
        Integer settled = settledAtTick.get(group);
        if (settled != null && settled == tick) {
            return false;
        }
        settledAtTick.put(group, tick);
        return true;
    }

    /**
     * Folds every member's change since the last settle into the pool and
     * returns the value to write to all of them, or empty when nobody is left,
     * in which case the pool is forgotten so the next member to arrive seeds a
     * fresh one rather than being handed back the zero that emptied it.
     */
    public OptionalDouble settle(String group, List<Member> living) {
        if (living.isEmpty()) {
            poolByGroup.remove(group);
            return OptionalDouble.empty();
        }

        Double pooled = poolByGroup.get(group);
        double pool;
        if (pooled == null) {
            // Nobody was left a moment ago, so this member starts the pool over
            // rather than inheriting whatever emptied it.
            pool = living.getFirst().value();
        } else {
            pool = pooled + change(living);
        }

        double value = capped(pool, living);
        poolByGroup.put(group, value);
        return OptionalDouble.of(value);
    }

    /** Every member's change since the last settle, gathered before any of it is written back. */
    private double change(List<Member> living) {
        double losses = 0.0;
        double gains = 0.0;
        double largestGain = 0.0;
        for (Member member : living) {
            Double before = lastSettled.get(member.id());
            if (before == null) {
                // Just arrived, or just back on their feet: they adopt the pool
                // rather than bringing their own value into it.
                continue;
            }
            double delta = member.value() - before;
            if (delta < 0) {
                losses += delta;
            } else {
                gains += delta;
                largestGain = Math.max(largestGain, delta);
            }
        }
        return losses + (gainsAddUp ? gains : largestGain);
    }

    /** The pool held within zero and the smallest ceiling among these players. */
    private static double capped(double pool, List<Member> living) {
        double ceiling = Double.MAX_VALUE;
        for (Member member : living) {
            ceiling = Math.min(ceiling, member.ceiling());
        }
        return Math.max(0.0, Math.min(pool, ceiling));
    }

    /** Drops everything, for a server stop or a plugin disable. */
    public void clear() {
        poolByGroup.clear();
        membersByGroup.clear();
        settledAtTick.clear();
        lastSettled.clear();
    }

    /** The ids in a group that the adapter reported as gone, so they can be forgotten in one pass. */
    public List<UUID> forgetAbsent(String group, Set<UUID> stillLiving) {
        List<UUID> absent = new ArrayList<>();
        for (UUID id : membersByGroup.getOrDefault(group, Set.of())) {
            if (!stillLiving.contains(id)) {
                absent.add(id);
            }
        }
        absent.forEach(lastSettled::remove);
        return absent;
    }
}
