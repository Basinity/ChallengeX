package com.basinity.challengex.fabric.lifecycle;

import java.util.function.Consumer;
import net.minecraft.server.MinecraftServer;

/**
 * The static seam the server-save Mixin emits through. The Mixin is woven into
 * {@code MinecraftServer} and cannot hold an adapter reference, so it reaches
 * run persistence through this holder, armed once at mod init with the same
 * save routine the lifecycle transitions use.
 *
 * <p>Emitting before the bridge is armed is a no-op, so a save firing early in
 * startup idles rather than throwing into vanilla code.
 */
public final class RunPersistenceBridge {

    private static volatile Consumer<MinecraftServer> onSave;

    private RunPersistenceBridge() {
    }

    public static void arm(Consumer<MinecraftServer> handler) {
        onSave = handler;
    }

    public static void onWorldSave(MinecraftServer server) {
        Consumer<MinecraftServer> current = onSave;
        if (current != null) {
            current.accept(server);
        }
    }
}
