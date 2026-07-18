package com.basinity.challengex.fabric.command;

import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.RunState;
import com.basinity.challengex.core.preset.Preset;
import com.basinity.challengex.core.preset.PresetCodec;
import com.basinity.challengex.core.preset.PresetFormatException;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.fabric.ChallengeXFabric;
import com.basinity.challengex.fabric.lifecycle.RunController;
import com.basinity.challengex.fabric.lifecycle.TimerColors;
import com.basinity.challengex.fabric.lifecycle.TimerPreferences;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * The {@code /challengex} command tree: the mod's real admin surface for loading
 * challenges without a server restart. {@code import} with no argument lists the
 * preset files in the config folder as clickable entries and prints the folder
 * path as click-to-copy text; {@code import <file>} parses that preset and swaps
 * it in as the active challenge; {@code reload} re-reads the last imported file.
 * Every mutating verb is gated on op level 2; {@code info} and {@code config},
 * which only read the run or edit the caller's own display preferences, are open
 * to everyone.
 *
 * <p>A rejected preset (unknown ids, bad scopes, malformed JSON) leaves the
 * active challenge untouched and prints every problem the codec found at once,
 * never a partial import.
 */
public final class ChallengeCommand {

    private final PresetStore store;
    private final PresetCodec codec;
    private final RunController controller;
    private final TimerPreferences preferences;

    /** The last preset imported this session, so {@code reload} knows what to re-read. */
    private String activePresetName;

    /** The companion web builder, where challenges are composed outside the game. */
    private static final String BUILDER_URL = "https://challengexmc.com";

    public ChallengeCommand(PresetStore store, RunController controller, TimerPreferences preferences) {
        this.store = store;
        this.controller = controller;
        this.preferences = preferences;
        this.codec = new PresetCodec(CoreCatalog.createRegistries());
    }

    /**
     * The tree root carries no permission gate: every mutating leaf (import,
     * reload, start, reset, pause, resume) gates itself on op level 2, while the
     * read-only {@code info} view and the personal {@code config} preferences
     * stay open to all players. That is what lets the clickable "view
     * configuration" control on the run-end message work for everyone in the
     * run, not just the operator, and what lets every player pick their own
     * clock color or hide the clock without needing op.
     */
    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("challengex")
                        .executes(this::about)
                        .then(Commands.literal("import").requires(Perms.requireAdmin())
                                .executes(this::listPresets)
                                .then(Commands.argument("file", StringArgumentType.greedyString())
                                        .suggests(this::suggestPresets)
                                        .executes(this::importNamed)))
                        .then(Commands.literal("reload").requires(Perms.requireAdmin())
                                .executes(this::reload))
                        .then(Commands.literal("start").requires(Perms.requireAdmin())
                                .executes(this::start))
                        .then(Commands.literal("reset").requires(Perms.requireAdmin())
                                .executes(this::reset))
                        .then(Commands.literal("pause").requires(Perms.requireAdmin())
                                .executes(this::pause))
                        .then(Commands.literal("resume").requires(Perms.requireAdmin())
                                .executes(this::resume))
                        .then(Commands.literal("config")
                                .executes(this::configShow)
                                .then(Commands.literal("timer_color")
                                        .executes(this::timerColorShow)
                                        .then(Commands.argument("color", StringArgumentType.word())
                                                .suggests(this::suggestColors)
                                                .executes(this::timerColorSet)))
                                .then(Commands.literal("hide_timer")
                                        .executes(this::hideTimerShow)
                                        .then(Commands.argument("hidden", BoolArgumentType.bool())
                                                .executes(this::hideTimerSet))))
                        .then(Commands.literal("info")
                                .executes(this::info))));
    }

    /**
     * The bare {@code /challengex} landing line: what the mod is, plus a clickable
     * link to the web builder. It is ungated so every player, not just the
     * operator, has an in-game path to where challenges are built without having to
     * find the Modrinth listing first.
     */
    private int about(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> Component.literal(
                "ChallengeX: compose your own challenge from rules, goals, and modifiers.")
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(this::builderLink, false);
        return 1;
    }

    private int listPresets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<String> names = store.listPresetNames();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No presets found. Drop a preset JSON into your presets folder, then run /challengex import <file>.")
                    .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(this::folderCopyLine, false);
            source.sendSuccess(this::builderLink, false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Presets (click to import):").withStyle(ChatFormatting.GOLD), false);
        for (String name : names) {
            source.sendSuccess(() -> importLink(name), false);
        }
        source.sendSuccess(this::folderCopyLine, false);
        source.sendSuccess(this::builderLink, false);
        return 1;
    }

    private int importNamed(CommandContext<CommandSourceStack> context) {
        return doImport(context.getSource(), StringArgumentType.getString(context, "file"));
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (activePresetName == null) {
            source.sendFailure(Component.literal("No preset imported yet. Use /challengex import <file> first."));
            return 0;
        }
        return doImport(source, activePresetName);
    }

    /** Reads, parses, and applies a preset; leaves the active challenge untouched on any failure. */
    private int doImport(CommandSourceStack source, String name) {
        Optional<String> json = store.read(name);
        if (json.isEmpty()) {
            source.sendFailure(Component.literal("No preset '" + name + "' in " + store.displayPath()));
            return 0;
        }
        Preset preset;
        try {
            preset = codec.fromJson(json.get());
        } catch (PresetFormatException rejected) {
            source.sendFailure(Component.literal("Could not import '" + name + "':"));
            for (String problem : rejected.problems()) {
                source.sendFailure(Component.literal("  - " + problem));
            }
            return 0;
        }
        ChallengeXFabric.instance().loadChallenge(preset.challenge());
        controller.onChallengeReplaced(source.getServer());
        activePresetName = name;
        source.sendSuccess(() -> Component.literal(
                "Imported '" + preset.name() + "'. Run /challengex start to begin.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    /** A run wrapping the empty starting challenge: nothing has been imported yet. */
    private static boolean noChallengeLoaded(ChallengeRun run) {
        return run == null || (run.challenge().rules().isEmpty()
                && run.challenge().goal().isEmpty() && run.challenge().modifiers().isEmpty());
    }

    private int start(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ChallengeRun run = ChallengeXFabric.instance().activeRun();
        if (noChallengeLoaded(run)) {
            source.sendFailure(Component.literal("No challenge loaded. Use /challengex import <file> first."));
            return 0;
        }
        switch (run.state()) {
            case RUNNING, PAUSED -> {
                source.sendFailure(Component.literal("A challenge is already running. Use /challengex reset first."));
                return 0;
            }
            case FINISHED -> {
                source.sendFailure(Component.literal("This run has finished. Use /challengex reset to play it again."));
                return 0;
            }
            case NOT_STARTED -> {
                controller.start(source.getServer());
                source.sendSuccess(() -> Component.literal("Challenge started.").withStyle(ChatFormatting.GREEN), true);
                return 1;
            }
        }
        return 0;
    }

    private int reset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        controller.reset(source.getServer());
        source.sendSuccess(() -> Component.literal("Challenge reset. Run /challengex start to begin again.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private int pause(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ChallengeRun run = ChallengeXFabric.instance().activeRun();
        if (run == null || run.state() != RunState.RUNNING) {
            source.sendFailure(Component.literal("No running challenge to pause."));
            return 0;
        }
        controller.pause(source.getServer());
        source.sendSuccess(() -> Component.literal("Challenge paused.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private int resume(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ChallengeRun run = ChallengeXFabric.instance().activeRun();
        if (run == null || run.state() != RunState.PAUSED) {
            source.sendFailure(Component.literal("No paused challenge to resume."));
            return 0;
        }
        controller.resume(source.getServer());
        source.sendSuccess(() -> Component.literal("Challenge resumed.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ChallengeRun run = ChallengeXFabric.instance().activeRun();
        if (noChallengeLoaded(run)) {
            source.sendSuccess(() -> Component.literal("No challenge loaded.").withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }
        for (Component line : ChallengeSummary.describe(run.challenge(), activePresetName, run.state())) {
            source.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private int configShow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<ServerPlayer> player = caller(source);
        if (player.isEmpty()) {
            return 0;
        }
        UUID id = player.get().getUUID();
        source.sendSuccess(() -> Component.literal("Your timer color: " + preferences.timerColor(id))
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("Your timer hidden: " + preferences.hideTimer(id))
                .withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("These are your own settings and affect nobody else."
                + " Change them with /challengex config timer_color <color>"
                + " and /challengex config hide_timer <true|false>.")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private int timerColorShow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<ServerPlayer> player = caller(source);
        if (player.isEmpty()) {
            return 0;
        }
        UUID id = player.get().getUUID();
        source.sendSuccess(() -> Component.literal("Your timer color: " + preferences.timerColor(id)
                + " (click a color to use it):").withStyle(ChatFormatting.GOLD), false);
        for (String color : TimerColors.names()) {
            source.sendSuccess(() -> colorLink(color), false);
        }
        return 1;
    }

    private int timerColorSet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<ServerPlayer> player = caller(source);
        if (player.isEmpty()) {
            return 0;
        }
        String color = StringArgumentType.getString(context, "color");
        if (!preferences.setTimerColor(player.get().getUUID(), color)) {
            source.sendFailure(Component.literal("Unknown color '" + color + "'. Options: "
                    + String.join(", ", TimerColors.names())));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Your timer color is now " + color + ".")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private int hideTimerShow(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<ServerPlayer> player = caller(source);
        if (player.isEmpty()) {
            return 0;
        }
        UUID id = player.get().getUUID();
        source.sendSuccess(() -> Component.literal("Your timer hidden: " + preferences.hideTimer(id))
                .withStyle(ChatFormatting.GOLD), false);
        return 1;
    }

    private int hideTimerSet(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Optional<ServerPlayer> player = caller(source);
        if (player.isEmpty()) {
            return 0;
        }
        boolean hidden = BoolArgumentType.getBool(context, "hidden");
        preferences.setHideTimer(player.get().getUUID(), hidden);
        source.sendSuccess(() -> Component.literal(hidden
                ? "Your timer is now hidden." : "Your timer is now shown.")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /**
     * The player whose preferences a {@code config} command reads or edits. These
     * settings belong to a player, so the console has none to show or change.
     */
    private Optional<ServerPlayer> caller(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.literal(
                    "/challengex config sets your own display settings, so a player has to run it."));
        }
        return Optional.ofNullable(player);
    }

    private Component colorLink(String color) {
        return Component.literal("  • " + color).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent.RunCommand("/challengex config timer_color " + color))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Use the " + color + " timer"))));
    }

    private CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String color : TimerColors.names()) {
            if (color.startsWith(prefix)) {
                builder.suggest(color);
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestPresets(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : store.listPresetNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private Component importLink(String name) {
        return Component.literal("  • " + name).withStyle(style -> style
                .withColor(ChatFormatting.YELLOW)
                .withClickEvent(new ClickEvent.RunCommand("/challengex import " + name))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to import " + name))));
    }

    /**
     * A single clickable label that copies the presets folder path to the
     * clipboard, to paste into a file browser. Minecraft flags the open-file
     * click action as not-allowed-from-server, so a server-side mod cannot make
     * the client open the folder directly; copy-to-clipboard is the closest the
     * platform allows.
     */
    private Component folderCopyLine() {
        String path = store.displayPath();
        return Component.literal("[Click to copy presets folder path]").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.CopyToClipboard(path))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(path))));
    }

    /**
     * A clickable link that opens the companion web builder in the player's
     * browser. Unlike the presets folder, which the client refuses to open from a
     * server, an https URL is an allowed open_url target, so this is the one thing
     * the game can point a player straight at to go build a challenge.
     */
    private Component builderLink() {
        return Component.literal("Build a challenge at " + BUILDER_URL).withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(BUILDER_URL)))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Open the ChallengeX web builder"))));
    }
}
