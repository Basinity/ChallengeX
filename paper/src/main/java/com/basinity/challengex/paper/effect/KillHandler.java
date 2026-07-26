package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** {@code effect.kill}: kills each target outright. */
public final class KillHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        for (Player target : targets) {
            target.setHealth(0.0);
        }
    }
}
