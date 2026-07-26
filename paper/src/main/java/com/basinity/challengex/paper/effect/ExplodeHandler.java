package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * {@code effect.explode}: sets off an explosion at each target's position with
 * the given {@code power} (default two, smaller than TNT's four). It breaks
 * blocks and damages entities like a real explosion, without setting fires,
 * which is what the Fabric side's TNT interaction does too.
 */
public final class ExplodeHandler implements EffectHandler {

    private static final double DEFAULT_POWER = 2.0;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        float power = (float) CatalogBounds.clampDouble(command.effectId(), "power",
                EffectParams.decimal(command, "power", DEFAULT_POWER));
        for (Player target : targets) {
            target.getWorld().createExplosion(target.getLocation(), power, false, true, target);
        }
    }
}
