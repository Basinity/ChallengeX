package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/** {@code effect.lightning}: strikes lightning at each target's position. */
public final class LightningHandler implements EffectHandler {

    private static final Identifier LIGHTNING_BOLT = Identifier.parse("minecraft:lightning_bolt");

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        EntityType<?> boltType = BuiltInRegistries.ENTITY_TYPE.get(LIGHTNING_BOLT)
                .map(Holder::value).orElse(null);
        if (boltType == null) {
            return;
        }
        for (ServerPlayer target : targets) {
            boltType.spawn(target.level(), target.blockPosition(), EntitySpawnReason.COMMAND);
        }
    }
}
