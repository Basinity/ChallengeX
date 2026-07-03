package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** {@code effect.spawn_mob}: spawns the named mob at each target's feet, count times. */
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
        int count = EffectParams.clamp(EffectParams.integer(command, "count", 1), 1, 100);
        for (ServerPlayer target : targets) {
            ServerLevel level = target.level();
            for (int i = 0; i < count; i++) {
                type.spawn(level, target.blockPosition(), EntitySpawnReason.COMMAND);
            }
        }
    }
}
