package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/** {@code effect.teleport_random}: teleports each target to a random spot within the radius. */
public final class TeleportRandomHandler implements EffectHandler {

    private static final int DEFAULT_RADIUS = 100;

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        int radius = EffectParams.clamp(EffectParams.integer(command, "radius", DEFAULT_RADIUS), 1, 100_000);
        for (ServerPlayer target : targets) {
            RandomSource random = target.getRandom();
            int x = (int) target.getX() + random.nextInt(radius * 2 + 1) - radius;
            int z = (int) target.getZ() + random.nextInt(radius * 2 + 1) - radius;
            ServerLevel level = target.level();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            target.teleportTo(x + 0.5, y, z + 0.5);
        }
    }
}
