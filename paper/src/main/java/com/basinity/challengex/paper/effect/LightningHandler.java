package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** {@code effect.lightning}: strikes lightning at each target's position. */
public final class LightningHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        for (Player target : targets) {
            target.getWorld().strikeLightning(target.getLocation());
        }
    }
}
