package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code effect.knockback}: shoves each target in a random horizontal direction
 * with the given {@code strength} (default one), tossing them a little upward
 * with it.
 */
public final class KnockbackHandler implements EffectHandler {

    private static final double DEFAULT_STRENGTH = 1.0;
    private static final double UPWARD = 0.4;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        double strength = Math.max(0.0, EffectParams.decimal(command, "strength", DEFAULT_STRENGTH));
        for (ServerPlayer target : targets) {
            double angle = target.getRandom().nextDouble() * 2.0 * Math.PI;
            target.setDeltaMovement(Math.cos(angle) * strength, UPWARD, Math.sin(angle) * strength);
            // Forces a velocity packet so the client actually moves.
            target.hurtMarked = true;
        }
    }
}
