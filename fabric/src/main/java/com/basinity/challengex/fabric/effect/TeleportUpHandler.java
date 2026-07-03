package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** {@code effect.teleport_up}: teleports each target the given number of blocks straight up. */
public final class TeleportUpHandler implements EffectHandler {

    private static final int DEFAULT_BLOCKS = 10;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int blocks = EffectParams.clamp(EffectParams.integer(command, "blocks", DEFAULT_BLOCKS), 0, 1024);
        for (ServerPlayer target : targets) {
            target.teleportTo(target.getX(), target.getY() + blocks, target.getZ());
        }
    }
}
