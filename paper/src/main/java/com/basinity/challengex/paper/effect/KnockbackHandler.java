package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * {@code effect.knockback}: shoves each target in a random horizontal direction
 * with the given {@code strength} (default one), tossing them a little upward
 * with it.
 */
public final class KnockbackHandler implements EffectHandler {

    private static final double DEFAULT_STRENGTH = 1.0;
    private static final double UPWARD = 0.4;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        double strength = CatalogBounds.clampDouble(command.effectId(), "strength",
                EffectParams.decimal(command, "strength", DEFAULT_STRENGTH));
        for (Player target : targets) {
            double angle = ThreadLocalRandom.current().nextDouble() * 2.0 * Math.PI;
            target.setVelocity(new Vector(Math.cos(angle) * strength, UPWARD, Math.sin(angle) * strength));
        }
    }
}
