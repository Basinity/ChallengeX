package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.engine.EffectExecutor;
import com.basinity.challengex.fabric.effect.EffectHandler;
import com.basinity.challengex.fabric.effect.EffectHandlers;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

/**
 * Fabric's half of the adapter contract. It resolves the command's target to
 * online players and dispatches to the {@link EffectHandler} registered for the
 * effect id; the handler applies the effect against the game.
 *
 * <p>An effect id with no registered handler logs and is skipped rather than
 * failing the run: the effect catalog is wired incrementally across the
 * building-block-library phase.
 */
final class FabricEffectExecutor implements EffectExecutor {

    private final MinecraftServer server;
    private final Logger logger;
    private final Map<String, EffectHandler> handlers;

    FabricEffectExecutor(MinecraftServer server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.handlers = EffectHandlers.byId();
    }

    @Override
    public void execute(EffectCommand command) {
        EffectHandler handler = handlers.get(command.effectId());
        if (handler == null) {
            logger.warn("Effect {} is not wired in the Fabric adapter yet.", command.effectId());
            return;
        }
        handler.execute(command, resolve(command.target()), server);
    }

    private List<ServerPlayer> resolve(EffectCommand.Target target) {
        return switch (target) {
            case EffectCommand.Target.AllPlayers ignored -> List.copyOf(server.getPlayerList().getPlayers());
            case EffectCommand.Target.Players players -> players.playerIds().stream()
                    .map(server.getPlayerList()::getPlayerByName)
                    .filter(Objects::nonNull)
                    .toList();
        };
    }
}
