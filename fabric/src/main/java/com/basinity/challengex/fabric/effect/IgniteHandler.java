package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** {@code effect.ignite}: sets each target on fire for the given seconds. */
public final class IgniteHandler implements EffectHandler {

    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_SECONDS = 5;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int seconds = Math.max(0, EffectParams.integer(command, "seconds", DEFAULT_SECONDS));
        for (ServerPlayer target : targets) {
            target.igniteForTicks(seconds * TICKS_PER_SECOND);
        }
    }
}
