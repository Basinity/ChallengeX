package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** {@code effect.drop_inventory}: drops each target's entire inventory on the ground. */
public final class DropInventoryHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        for (ServerPlayer target : targets) {
            target.getInventory().dropAll();
        }
    }
}
