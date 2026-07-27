package com.basinity.challengex.paper.modifier;

import org.bukkit.entity.Player;

/**
 * {@code modifier.share_hunger}: the scoped players share one hunger bar.
 * Eating fills it for everyone and any of them getting hungry empties it for
 * everyone.
 *
 * <p>Only the bar itself is pooled. Saturation and exhaustion stay each
 * player's own, so the group's bar drains faster the more of them there are,
 * which is what one bar feeding several people should do.
 */
public final class SharedHungerEnforcer extends SharedPoolEnforcer {

    /** A full hunger bar, which is the same for every player. */
    private static final double FULL_BAR = 20.0;

    @Override
    protected double read(Player player) {
        return player.getFoodLevel();
    }

    @Override
    protected void write(Player player, double value) {
        player.setFoodLevel((int) Math.round(value));
    }

    @Override
    protected double ceiling(Player player) {
        return FULL_BAR;
    }
}
