package com.basinity.challengex.paper.effect;

import com.basinity.challengex.common.effect.EffectParams;
import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.registry.CatalogBounds;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/** {@code effect.ignite}: sets each target on fire for the given seconds. */
public final class IgniteHandler implements EffectHandler {

    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_SECONDS = 5;

    @Override
    public void execute(EffectCommand command, List<Player> targets, Server server) {
        int seconds = CatalogBounds.clampInt(command.effectId(), "seconds",
                EffectParams.integer(command, "seconds", DEFAULT_SECONDS));
        for (Player target : targets) {
            target.setFireTicks(seconds * TICKS_PER_SECOND);
        }
    }
}
