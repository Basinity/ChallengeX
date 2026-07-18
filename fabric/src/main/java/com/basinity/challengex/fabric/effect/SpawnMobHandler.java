package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code effect.spawn_mob}: spawns the named mob at each target's feet, count
 * times. With {@code baby} true, ageable mobs spawn as babies.
 */
public final class SpawnMobHandler implements EffectHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpawnMobHandler.class);

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String mobId = EffectParams.string(command, "mob");
        if (mobId == null) {
            LOGGER.warn("spawn_mob is missing its mob id; skipping.");
            return;
        }
        Identifier id = Identifier.tryParse(mobId);
        EntityType<?> type = id == null ? null
                : BuiltInRegistries.ENTITY_TYPE.get(id).map(Holder::value).orElse(null);
        if (type == null) {
            LOGGER.warn("Unknown mob {}; skipping.", mobId);
            return;
        }
        int count = CatalogBounds.clampInt(command.effectId(), "count",
                EffectParams.integer(command, "count", 1));
        boolean baby = EffectParams.bool(command, "baby", false);
        for (ServerPlayer target : targets) {
            ServerLevel level = target.level();
            for (int i = 0; i < count; i++) {
                Entity spawned = type.spawn(level, target.blockPosition(), EntitySpawnReason.COMMAND);
                if (baby && spawned instanceof AgeableMob ageable) {
                    ageable.setBaby(true);
                }
            }
        }
    }
}
