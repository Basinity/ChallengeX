package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code effect.heal}: heals each target. With a {@code hearts} amount it heals
 * that many hearts (the game caps it at full); without one it restores full
 * health. Negative amounts clamp to zero.
 */
public final class HealHandler implements EffectHandler {

    private static final double MAX_HEARTS = 1024.0;
    private static final double HALF_HEARTS_PER_HEART = 2.0;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        boolean hasHearts = EffectParams.has(command, "hearts");
        double hearts = EffectParams.clamp(EffectParams.decimal(command, "hearts", 0.0), 0.0, MAX_HEARTS);
        for (ServerPlayer target : targets) {
            if (hasHearts) {
                target.heal((float) (hearts * HALF_HEARTS_PER_HEART));
            } else {
                target.setHealth(target.getMaxHealth());
            }
        }
    }
}
