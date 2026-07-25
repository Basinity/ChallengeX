package com.basinity.challengex.fabric.lifecycle;

import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Resolves the world folder the run snapshot lives under. This is the one part
 * of run persistence that differs per platform, which is why the shared
 * {@code RunStore} takes the path rather than the server.
 */
public final class WorldPaths {

    private WorldPaths() {
    }

    public static Path root(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT);
    }
}
