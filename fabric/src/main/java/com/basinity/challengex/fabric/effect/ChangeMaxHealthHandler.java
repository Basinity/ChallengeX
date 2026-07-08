package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * {@code effect.change_max_health}: shifts each target's maximum health by the
 * required {@code hearts} amount, positive to add hearts or negative to remove
 * them. The change is an additive edit to the max-health attribute's base value,
 * so it persists with the player and re-triggering stacks. A never-below floor of
 * one heart keeps max health out of the degenerate zero state, and current health
 * is clamped down when the new maximum is lower.
 */
public final class ChangeMaxHealthHandler implements EffectHandler {

    private static final double HALF_HEARTS_PER_HEART = 2.0;
    private static final double MIN_MAX_HEALTH = 2.0;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        double halfHearts = EffectParams.decimal(command, "hearts", 0.0) * HALF_HEARTS_PER_HEART;
        for (ServerPlayer target : targets) {
            AttributeInstance attribute = target.getAttribute(Attributes.MAX_HEALTH);
            if (attribute == null) {
                continue;
            }
            double newMax = Math.max(MIN_MAX_HEALTH, attribute.getBaseValue() + halfHearts);
            attribute.setBaseValue(newMax);
            if (target.getHealth() > newMax) {
                target.setHealth((float) newMax);
            }
        }
    }
}
