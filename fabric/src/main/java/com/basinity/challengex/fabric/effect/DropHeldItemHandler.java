package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code effect.drop_held_item}: drops the selected slot. The {@code stack}
 * flag (default true) drops the whole stack; false drops a single item. The
 * drop is always thrown from the hand; {@code false} to {@code drop} only means
 * it does not scatter randomly.
 */
public final class DropHeldItemHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        boolean wholeStack = EffectParams.bool(command, "stack", true);
        for (ServerPlayer target : targets) {
            target.drop(target.getInventory().removeFromSelected(wholeStack), false);
        }
    }
}
