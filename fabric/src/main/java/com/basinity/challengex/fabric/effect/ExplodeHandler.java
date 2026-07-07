package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * {@code effect.explode}: sets off an explosion at each target's position with
 * the given {@code power} (default two, smaller than TNT's four). It breaks
 * blocks and damages entities like a real explosion.
 */
public final class ExplodeHandler implements EffectHandler {

    private static final double DEFAULT_POWER = 2.0;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        float power = (float) Math.max(0.0, EffectParams.decimal(command, "power", DEFAULT_POWER));
        for (ServerPlayer target : targets) {
            target.level().explode(target, target.getX(), target.getY(), target.getZ(),
                    power, Level.ExplosionInteraction.TNT);
        }
    }
}
