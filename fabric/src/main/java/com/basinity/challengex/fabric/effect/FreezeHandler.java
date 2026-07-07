package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * {@code effect.freeze}: roots each target in place for {@code seconds} (default
 * three). It applies overwhelming slowness, which halts walking; a full lock of
 * jumping and looking is the pause system's job (phase 6), not this effect's.
 */
public final class FreezeHandler implements EffectHandler {

    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_SECONDS = 3;
    // High enough that the movement-speed attribute floors at zero.
    private static final int ROOT_AMPLIFIER = 250;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int seconds = Math.max(1, EffectParams.integer(command, "seconds", DEFAULT_SECONDS));
        for (ServerPlayer target : targets) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, seconds * TICKS_PER_SECOND, ROOT_AMPLIFIER));
        }
    }
}
