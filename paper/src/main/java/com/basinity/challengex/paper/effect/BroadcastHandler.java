package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * {@code effect.broadcast}: sends a chat message to every player. Playerless, so
 * the executor resolves its target to everyone online.
 */
public final class BroadcastHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        String text = EffectParams.string(command, "text");
        if (text == null) {
            return;
        }
        Component message = Component.text(text);
        for (Player target : targets) {
            target.sendMessage(message);
        }
    }
}
