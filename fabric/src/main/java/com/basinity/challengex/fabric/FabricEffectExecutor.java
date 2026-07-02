package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.engine.EffectExecutor;
import com.basinity.challengex.core.model.ParamValue;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.slf4j.Logger;

/**
 * Fabric's half of the adapter contract. It turns an engine {@link EffectCommand}
 * into real game state: it resolves the command's target to online players and
 * applies the effect against them.
 *
 * <p>Only {@code effect.apply_status_effect} is wired for the vertical slice;
 * the rest of the effect catalog lands in the building-block-library phase. An
 * unwired effect id logs and is skipped rather than failing the run.
 */
final class FabricEffectExecutor implements EffectExecutor {

    private static final int DEFAULT_STATUS_DURATION_TICKS = 200;

    private final MinecraftServer server;
    private final Logger logger;

    FabricEffectExecutor(MinecraftServer server, Logger logger) {
        this.server = Objects.requireNonNull(server, "server");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    public void execute(EffectCommand command) {
        List<ServerPlayer> targets = resolve(command.target());
        if (targets.isEmpty()) {
            return;
        }
        switch (command.effectId()) {
            case "effect.apply_status_effect" -> applyStatusEffect(command, targets);
            default -> logger.warn("Effect {} is not wired in the Fabric adapter yet.", command.effectId());
        }
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

    private void applyStatusEffect(EffectCommand command, List<ServerPlayer> targets) {
        String effectId = stringParam(command, "effect");
        if (effectId == null) {
            logger.warn("apply_status_effect is missing its effect id; skipping.");
            return;
        }
        Identifier id = Identifier.tryParse(effectId);
        Holder<MobEffect> effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
        if (effect == null) {
            logger.warn("Unknown status effect {}; skipping.", effectId);
            return;
        }
        int duration = intParam(command, "duration", DEFAULT_STATUS_DURATION_TICKS);
        int amplifier = intParam(command, "amplifier", 0);
        for (ServerPlayer target : targets) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    private static String stringParam(EffectCommand command, String name) {
        return command.params().get(name) instanceof ParamValue.OfString string ? string.value() : null;
    }

    private static int intParam(EffectCommand command, String name, int fallback) {
        return command.params().get(name) instanceof ParamValue.OfInt integer ? (int) integer.value() : fallback;
    }
}
