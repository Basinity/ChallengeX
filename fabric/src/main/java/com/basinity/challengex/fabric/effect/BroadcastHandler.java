package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code effect.broadcast}: sends a chat message to every player. Playerless, so
 * the executor resolves its target to everyone online.
 */
public final class BroadcastHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server) {
        String text = EffectParams.string(command, "text");
        if (text == null) {
            return;
        }
        Component message = Component.literal(text);
        for (ServerPlayer target : targets) {
            target.sendSystemMessage(message);
        }
    }
}
