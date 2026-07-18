package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** {@code effect.damage}: deals the given hearts of generic damage to each target. */
public final class DamageHandler implements EffectHandler {

    private static final double DEFAULT_HEARTS = 1.0;
    private static final double HALF_HEARTS_PER_HEART = 2.0;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        double hearts = CatalogBounds.clampDouble(command.effectId(), "hearts",
                EffectParams.decimal(command, "hearts", DEFAULT_HEARTS));
        float amount = (float) (hearts * HALF_HEARTS_PER_HEART);
        for (ServerPlayer target : targets) {
            ServerLevel level = target.level();
            target.hurtServer(level, level.damageSources().generic(), amount);
        }
    }
}
