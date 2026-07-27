package com.basinity.challengex.paper.modifier;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * {@code modifier.share_health}: the scoped players share one health pool.
 * Damage to any of them takes the group down, healing any of them brings the
 * group up, and when the pool reaches zero everybody in it dies, because there
 * is only the one pool to run out.
 *
 * <p>The cap is the smallest maximum health in the group, so a player whose
 * maximum has been lowered holds the pool down to what they can actually show.
 */
public final class SharedHealthEnforcer extends SharedPoolEnforcer {

    private static final double FALLBACK_MAX_HEALTH = 20.0;

    @Override
    protected double read(Player player) {
        return player.getHealth();
    }

    @Override
    protected void write(Player player, double value) {
        // Writing zero is what kills them, which is the point: the pool running
        // out has to end the run for everyone sharing it, not just the one who
        // took the last hit.
        player.setHealth(Math.max(0.0, Math.min(value, ceiling(player))));
    }

    @Override
    protected double ceiling(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? FALLBACK_MAX_HEALTH : attribute.getValue();
    }

    /**
     * Health is the one pool whose gains must not add up. Every member
     * regenerates on their own from a full hunger bar, so counting all of those
     * would heal a pair twice as fast as a single player, and a group of four
     * four times as fast. The pool comes back at the rate of whoever is
     * recovering quickest, which is what one bar would do.
     */
    @Override
    protected boolean gainsAddUp() {
        return false;
    }
}
