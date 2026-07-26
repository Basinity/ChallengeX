package com.basinity.challengex.paper.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Carries out one catalog effect against the players its command already
 * resolved to. One handler per effect id; {@link EffectHandlers} maps the ids
 * to handlers and {@code PaperEffectExecutor} dispatches to them.
 *
 * <p>Player effects act on {@code targets}; world- or run-level effects
 * (broadcast, change time) ignore the players and act through {@code server}.
 */
@FunctionalInterface
public interface EffectHandler {

    void execute(EffectCommand command, List<Player> targets, Server server);
}
