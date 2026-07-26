package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** {@code effect.drop_held_item}: throws the selected slot's whole stack out of the hand. */
public final class DropHeldItemHandler implements EffectHandler {

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        for (Player target : targets) {
            target.dropItem(true);
        }
    }
}
