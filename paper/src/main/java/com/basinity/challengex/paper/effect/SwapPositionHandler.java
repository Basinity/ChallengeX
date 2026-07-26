package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * {@code effect.swap_position}: teleports each target to a random other online
 * player's position and that player to the target's, across worlds.
 */
public final class SwapPositionHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        Set<Player> swapped = new HashSet<>();
        for (Player target : targets) {
            if (swapped.contains(target)) {
                continue;
            }
            Player other = Players.randomOther(target, server, swapped);
            if (other != null) {
                Location first = target.getLocation();
                target.teleport(other.getLocation());
                other.teleport(first);
                swapped.add(target);
                swapped.add(other);
            }
        }
    }
}
