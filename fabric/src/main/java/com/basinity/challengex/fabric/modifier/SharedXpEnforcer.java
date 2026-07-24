package com.basinity.challengex.fabric.modifier;

import net.minecraft.server.level.ServerPlayer;

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
    protected double read(ServerPlayer player) {
        return player.totalExperience;
    }

    @Override
    protected void write(ServerPlayer player, double value) {
        int total = (int) Math.round(value);
        if (player.totalExperience == total) {
            return;
        }
        // There is no setter for a total, so the counters are cleared and the
        // whole amount is granted again, which is what recomputes the level and
        // the bar and sends them to the client.
        player.totalExperience = 0;
        player.experienceLevel = 0;
        player.experienceProgress = 0.0f;
        player.giveExperiencePoints(total);
    }

    @Override
    protected double ceiling(ServerPlayer player) {
        return Double.MAX_VALUE;
    }
}
