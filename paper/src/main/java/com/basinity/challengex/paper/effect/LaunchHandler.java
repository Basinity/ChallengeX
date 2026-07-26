package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/** {@code effect.launch}: flings each target upward with the given strength. */
public final class LaunchHandler implements EffectHandler {

    private static final double DEFAULT_STRENGTH = 1.5;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        double strength = CatalogBounds.clampDouble(command.effectId(), "strength",
                EffectParams.decimal(command, "strength", DEFAULT_STRENGTH));
        for (Player target : targets) {
            // setVelocity already sends the velocity packet, so unlike the
            // Fabric side there is no marker to set for the client to move.
            target.setVelocity(new Vector(0.0, strength, 0.0));
        }
    }
}
