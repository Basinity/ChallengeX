package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** {@code effect.damage}: deals the given hearts of generic damage to each target. */
public final class DamageHandler implements EffectHandler {

    private static final double DEFAULT_HEARTS = 1.0;
    private static final double HALF_HEARTS_PER_HEART = 2.0;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        double hearts = CatalogBounds.clampDouble(command.effectId(), "hearts",
                EffectParams.decimal(command, "hearts", DEFAULT_HEARTS));
        double amount = hearts * HALF_HEARTS_PER_HEART;
        for (Player target : targets) {
            target.damage(amount);
        }
    }
}
