package com.basinity.challengex.paper.modifier;

import org.bukkit.entity.Player;

/**
 * {@code modifier.share_xp}: the scoped players share one experience pool.
 * Every orb one of them picks up is credited to all of them, and every level
 * one of them spends is spent by all of them.
 *
 * <p>The total is what is pooled rather than the level, because levels are not
 * evenly sized: pooling the level would make a level bought at one price
 * refundable at another.
 */
public final class SharedXpEnforcer extends SharedPoolEnforcer {

    @Override
    protected double read(Player player) {
        return player.getTotalExperience();
    }

    @Override
    protected void write(Player player, double value) {
        int total = (int) Math.round(value);
        if (player.getTotalExperience() == total) {
            return;
        }
        // setTotalExperience alone leaves the level and the bar showing the old
        // amount, so the counters are cleared and the whole amount granted
        // again, which is what recomputes both and sends them to the client.
        player.setTotalExperience(0);
        player.setLevel(0);
        player.setExp(0.0f);
        player.giveExp(total);
    }

    @Override
    protected double ceiling(Player player) {
        return Double.MAX_VALUE;
    }
}
