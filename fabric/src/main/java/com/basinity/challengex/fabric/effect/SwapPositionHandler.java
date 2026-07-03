package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code effect.swap_position}: teleports each target to a random other online
 * player's position and that player to the target's, across dimensions.
 */
public final class SwapPositionHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        for (ServerPlayer target : targets) {
            ServerPlayer other = Players.randomOther(target, server);
            if (other != null) {
                swapPositions(target, other);
            }
        }
    }

    private static void swapPositions(ServerPlayer first, ServerPlayer second) {
        ServerLevel firstLevel = first.level();
        double firstX = first.getX();
        double firstY = first.getY();
        double firstZ = first.getZ();
        float firstYaw = first.getYRot();
        float firstPitch = first.getXRot();

        first.teleportTo(second.level(), second.getX(), second.getY(), second.getZ(),
                Set.of(), second.getYRot(), second.getXRot(), false);
        second.teleportTo(firstLevel, firstX, firstY, firstZ,
                Set.of(), firstYaw, firstPitch, false);
    }
}
