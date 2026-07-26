package com.basinity.challengex.paper;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.engine.EffectExecutor;
import com.basinity.challengex.paper.effect.EffectHandler;
import com.basinity.challengex.paper.effect.EffectHandlers;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

/**
 * Paper's half of the adapter contract. It resolves the command's target to
 * online players and dispatches to the {@link EffectHandler} registered for the
 * effect id; the handler applies the effect against the game.
 *
 * <p>An effect id with no registered handler logs and is skipped rather than
 * failing the run. On this adapter that is not only a wiring gap: an effect the
 * Bukkit API cannot express is cut, and this is where a preset using it lands.
 */
final class PaperEffectExecutor implements EffectExecutor {

    private final Server server;
    private final Logger logger;
    private final Map<String, EffectHandler> handlers;

    PaperEffectExecutor(Server server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.handlers = EffectHandlers.byId();
    }

    @Override
    public void execute(EffectCommand command) {
        EffectHandler handler = handlers.get(command.effectId());
        if (handler == null) {
            logger.warn("Effect {} is not supported by the Paper adapter.", command.effectId());
            return;
        }
        handler.execute(command, resolve(command.target()), server);
    }

    private List<Player> resolve(EffectCommand.Target target) {
        return switch (target) {
            case EffectCommand.Target.AllPlayers ignored -> List.copyOf(server.getOnlinePlayers());
            case EffectCommand.Target.Players players -> players.playerIds().stream()
                    .map(server::getPlayerExact)
                    .filter(Objects::nonNull)
                    .toList();
        };
    }
}
