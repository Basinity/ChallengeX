package com.basinity.challengex.fabric;

import com.basinity.challengex.core.engine.EffectCommand;
import com.basinity.challengex.core.model.Challenge;
import com.basinity.challengex.core.model.EffectSpec;
import com.basinity.challengex.core.model.ParamValue;
import com.basinity.challengex.core.model.Rule;
import com.basinity.challengex.core.model.Scope;
import com.basinity.challengex.core.model.TriggerSpec;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * A throwaway dev harness for exercising the building-block libraries before the
 * real import path and command tree exist (phases 5 and 6). {@code /challengex-dev
 * fire <effect> [key=value ...]} runs a single effect against the sender,
 * {@code /challengex-dev watch on|off} echoes every fired trigger to chat so the
 * triggers can be checked in-world without a rule wired to each one, and
 * {@code /challengex-dev challenge slice|triggers} swaps the active challenge:
 * {@code slice} is the mob-kill-poisons-killer default, {@code triggers} configures
 * the five threshold and schedule triggers (which fire only when a rule watches
 * for them) so they can be exercised before preset import exists.
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
                                                        StringArgumentType.getString(context, "params"))))))
                        .then(Commands.literal("watch")
                                .then(Commands.literal("on").executes(context -> watch(context, true)))
                                .then(Commands.literal("off").executes(context -> watch(context, false))))
                        .then(Commands.literal("challenge")
                                .then(Commands.literal("slice").executes(context ->
                                        load(context, "slice", ChallengeXFabric.verticalSliceChallenge())))
                                .then(Commands.literal("triggers").executes(context ->
                                        load(context, "triggers", thresholdTestChallenge()))))));
    }

    private static int watch(CommandContext<CommandSourceStack> context, boolean on) {
        DevTriggerWatch.setEnabled(on);
        context.getSource().sendSuccess(
                () -> Component.literal("Trigger watch " + (on ? "on" : "off")), false);
        return 1;
    }

    private static int load(CommandContext<CommandSourceStack> context, String name, Challenge challenge) {
        ChallengeXFabric.instance().loadChallenge(challenge);
        context.getSource().sendSuccess(() -> Component.literal("Loaded challenge: " + name), false);
        return 1;
    }

    /**
     * The five threshold and schedule triggers, each paired with a broadcast that
     * names it, so both the trigger watch and the broadcast confirm a fire.
     * Thresholds are set loose so a little activity crosses them.
     */
    private static Challenge thresholdTestChallenge() {
        return new Challenge(List.of(
                watched("trigger.health_below", Map.of("hearts", ParamValue.of(9.0)), "health_below fired"),
                watched("trigger.hunger_below", Map.of("points", ParamValue.of(19L)), "hunger_below fired"),
                watched("trigger.height_crossed", Map.of("y", ParamValue.of(100L)), "height_crossed fired"),
                watched("trigger.time_of_day", Map.of("time", ParamValue.of("night")), "time_of_day night fired"),
                watched("trigger.time_of_day", Map.of("time", ParamValue.of("day")), "time_of_day day fired"),
                watched("trigger.fixed_interval", Map.of("seconds", ParamValue.of(15L)), "fixed_interval fired")),
                Optional.empty(), List.of());
    }

    private static Rule watched(String triggerId, Map<String, ParamValue> triggerParams, String text) {
        Optional<Scope.Absolute> scope = triggerId.equals("trigger.time_of_day")
                || triggerId.equals("trigger.fixed_interval")
                ? Optional.empty() : Optional.of(Scope.EVERY_PLAYER);
        return new Rule(
                new TriggerSpec(triggerId, triggerParams, scope),
                new EffectSpec("effect.broadcast", Map.of("text", ParamValue.of(text)),
                        Optional.of(Scope.EVERY_PLAYER)));
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
