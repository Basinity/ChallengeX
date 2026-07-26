package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** {@code effect.teleport_up}: teleports each target the given number of blocks straight up. */
public final class TeleportUpHandler implements EffectHandler {

    private static final int DEFAULT_BLOCKS = 10;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        int blocks = CatalogBounds.clampInt(command.effectId(), "blocks",
                EffectParams.integer(command, "blocks", DEFAULT_BLOCKS));
        for (Player target : targets) {
            Location destination = target.getLocation().add(0.0, blocks, 0.0);
            target.teleport(destination);
        }
    }
}
