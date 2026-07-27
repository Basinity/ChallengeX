package com.basinity.challengex.paper.modifier;

import com.basinity.challengex.common.modifier.SharedPool;
import com.basinity.challengex.core.model.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * The Paper half of a shared pool: read the value off a real player, write it
 * back, and say what their ceiling is. How a pool actually behaves is
 * {@link SharedPool}'s and is shared with the Fabric adapter.
 *
 * <p>A dead member is out of the pool until they are back on their feet.
 * Writing to a player who is still on the death screen leaves the server
 * believing they are fine while their client says otherwise, which strands
 * them; and once a whole group is down the pool is forgotten, so the first of
 * them to respawn seeds a fresh one instead of being handed back the zero that
 * killed them.
 */
abstract class SharedPoolEnforcer implements ModifierEnforcer {

    private final SharedPool pool = new SharedPool(gainsAddUp());

    /** The value being pooled, read off one player. */
    protected abstract double read(Player player);

    /** Puts the pooled value back on one player. */
    protected abstract void write(Player player, double value);

    /** The most this player can hold, or {@link Double#MAX_VALUE} when there is no cap. */
    protected abstract double ceiling(Player player);

    /** Whether several members gaining at once add up; see {@link SharedPool}. */
    protected boolean gainsAddUp() {
        return true;
    }

    @Override
    public void start(Player player, Modifier modifier, Server server) {
        String group = SharedPool.groupKey(modifier);
        pool.addMember(group, player.getUniqueId());
        if (player.isDead()) {
            return;
        }
        if (pool.hasPool(group)) {
            write(player, pool.adopt(group, ceiling(player)));
        } else {
            pool.seed(group, read(player));
        }
        pool.recordSettled(player.getUniqueId(), read(player));
    }

    @Override
    public void tick(Player player, Modifier modifier, Server server) {
        String group = SharedPool.groupKey(modifier);
        if (!pool.claimTick(group, server.getCurrentTick())) {
            return;
        }
        List<Player> living = livingMembers(group, server);
        List<SharedPool.Member> members = new ArrayList<>(living.size());
        for (Player member : living) {
            members.add(new SharedPool.Member(member.getUniqueId(), read(member), ceiling(member)));
        }
        OptionalDouble settled = pool.settle(group, members);
        if (settled.isEmpty()) {
            return;
        }
        for (Player member : living) {
            write(member, settled.getAsDouble());
            // Read back rather than trusting the write: the game may clamp it,
            // and a difference we did not notice would read as a change next tick.
            pool.recordSettled(member.getUniqueId(), read(member));
        }
    }

    @Override
    public void stop(Player player, Modifier modifier, Server server) {
        pool.leave(SharedPool.groupKey(modifier), player.getUniqueId());
    }

    @Override
    public void disabled() {
        pool.clear();
    }

    /**
     * The members who are online and alive. A dead one is forgotten as well as
     * skipped, so that when they respawn they read as having just arrived and
     * adopt the pool instead of contributing the whole of their restored value.
     */
    private List<Player> livingMembers(String group, Server server) {
        List<Player> living = new ArrayList<>();
        Set<UUID> stillLiving = new HashSet<>();
        for (UUID id : pool.members(group)) {
            Player player = server.getPlayer(id);
            if (player != null && player.isOnline() && !player.isDead()) {
                living.add(player);
                stillLiving.add(id);
            }
        }
        pool.forgetAbsent(group, stillLiving);
        return living;
    }
}
