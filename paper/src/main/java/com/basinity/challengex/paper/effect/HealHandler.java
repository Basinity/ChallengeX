package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

/**
 * {@code effect.heal}: heals each target. With a {@code hearts} amount it heals
 * that many hearts (capped at full); without one it restores full health.
 * Negative amounts clamp to zero.
 */
public final class HealHandler implements EffectHandler {

    private static final double HALF_HEARTS_PER_HEART = 2.0;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        boolean hasHearts = EffectParams.has(command, "hearts");
        double hearts = CatalogBounds.clampDouble(command.effectId(), "hearts",
                EffectParams.decimal(command, "hearts", 0.0));
        for (Player target : targets) {
            AttributeInstance attribute = target.getAttribute(Attribute.MAX_HEALTH);
            if (attribute == null) {
                continue;
            }
            double max = attribute.getValue();
            // Bukkit has no heal(); the cap the game applies to a heal is
            // reproduced here rather than being had for free.
            target.setHealth(hasHearts
                    ? Math.min(max, target.getHealth() + hearts * HALF_HEARTS_PER_HEART)
                    : max);
        }
    }
}
