package com.basinity.challengex.fabric.effect;

import com.basinity.challengex.core.engine.EffectCommand;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Carries out one catalog effect against the players its command already
 * resolved to. One handler per effect id; {@link EffectHandlers} maps the ids
 * to handlers and {@code FabricEffectExecutor} dispatches to them.
 *
 * <p>Player effects act on {@code targets}; world- or run-level effects
 * (broadcast, change time) ignore the players and act through {@code server}.
 */
@FunctionalInterface
public interface EffectHandler {

    void execute(EffectCommand command, List<ServerPlayer> targets, MinecraftServer server);
}
