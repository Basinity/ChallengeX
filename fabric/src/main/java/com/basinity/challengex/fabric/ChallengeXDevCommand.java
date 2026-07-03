package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.model.ParamValue;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A throwaway dev harness for exercising the building-block libraries before the
 * real import path and command tree exist (phases 5 and 6). {@code /challengex-dev
 * fire <effect> [key=value ...]} runs a single effect against the sender.
 *
 * <p>This is not the production command surface: it is gated only on being a
 * player and is removed once phase 5 lands the permission-node-backed tree.
 */
final class ChallengeXDevCommand {

    private ChallengeXDevCommand() {
    }

    static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("challengex-dev")
                        .requires(CommandSourceStack::isPlayer)
                        .then(Commands.literal("fire")
                                .then(Commands.argument("effect", StringArgumentType.word())
                                        .executes(context -> fire(context, ""))
                                        .then(Commands.argument("params", StringArgumentType.greedyString())
                                                .executes(context -> fire(context,
                                                        StringArgumentType.getString(context, "params"))))))));
    }

    private static int fire(CommandContext<CommandSourceStack> context, String rawParams) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String effectId = StringArgumentType.getString(context, "effect");
        if (!effectId.startsWith("effect.")) {
            effectId = "effect." + effectId;
        }
        EffectCommand command = new EffectCommand(effectId, parseParams(rawParams),
                EffectCommand.Target.player(player.getScoreboardName()));
        new FabricEffectExecutor(context.getSource().getServer(), ChallengeXFabric.LOGGER).execute(command);
        String fired = effectId;
        context.getSource().sendSuccess(() -> Component.literal("Fired " + fired), false);
        return 1;
    }

    /** Parses {@code key=value} tokens, inferring int, decimal, bool, then string. */
    private static Map<String, ParamValue> parseParams(String raw) {
        Map<String, ParamValue> params = new HashMap<>();
        if (raw.isBlank()) {
            return params;
        }
        for (String token : raw.trim().split("\\s+")) {
            int split = token.indexOf('=');
            if (split <= 0) {
                continue;
            }
            params.put(token.substring(0, split), parseValue(token.substring(split + 1)));
        }
        return params;
    }

    private static ParamValue parseValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return ParamValue.of(Boolean.parseBoolean(value));
        }
        try {
            return ParamValue.of(Long.parseLong(value));
        } catch (NumberFormatException ignoredInt) {
            // Not an integer; try a decimal next.
        }
        try {
            return ParamValue.of(Double.parseDouble(value));
        } catch (NumberFormatException ignoredDecimal) {
            // Not a number; a plain string.
        }
        return ParamValue.of(value);
    }
}
