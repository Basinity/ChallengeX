package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** {@code effect.teleport_random}: teleports each target to a random spot within the radius. */
public final class TeleportRandomHandler implements EffectHandler {

    private static final int DEFAULT_RADIUS = 100;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        int radius = CatalogBounds.clampInt(command.effectId(), "radius",
                EffectParams.integer(command, "radius", DEFAULT_RADIUS));
        for (Player target : targets) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            Location from = target.getLocation();
            int x = from.getBlockX() + random.nextInt(radius * 2 + 1) - radius;
            int z = from.getBlockZ() + random.nextInt(radius * 2 + 1) - radius;
            World world = target.getWorld();
            int y = world.getHighestBlockYAt(x, z);
            target.teleport(new Location(world, x + 0.5, y + 1.0, z + 0.5,
                    from.getYaw(), from.getPitch()));
        }
    }
}
