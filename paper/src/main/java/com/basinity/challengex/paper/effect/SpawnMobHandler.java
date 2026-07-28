package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code effect.spawn_mob}: spawns the named mob at each target's feet, count
 * times. With {@code baby} true, ageable mobs spawn as babies.
 */
public final class SpawnMobHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnMobHandler.class);

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String mobId = EffectParams.string(command, "mob");
        if (mobId == null) {
            LOGGER.warn("spawn_mob is missing its mob id; skipping.");
            return;
        }
        EntityType type = GameIds.entity(mobId);
        if (type == null) {
            LOGGER.warn("Unknown mob {}; skipping.", mobId);
            return;
        }
        // Suggestions never restrict what can be typed, so this id can name
        // something that is not spawnable. Bukkit throws for those where the
        // mod's spawn call simply returns nothing, and an exception here would
        // escape into the event that set the effect off.
        if (!type.isSpawnable()) {
            LOGGER.warn("{} cannot be spawned; skipping.", mobId);
            return;
        }
        int count = CatalogBounds.clampInt(command.effectId(), "count",
                EffectParams.integer(command, "count", 1));
        boolean baby = EffectParams.bool(command, "baby", false);
        for (Player target : targets) {
            for (int i = 0; i < count; i++) {
                Entity spawned = target.getWorld().spawnEntity(target.getLocation(), type);
                if (baby && spawned instanceof Ageable ageable) {
                    ageable.setBaby();
                }
            }
        }
    }
}
