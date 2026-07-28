package com.basinity.challengex.paper.command;

import com.basinity.challengex.common.command.ChallengeSummary;
import com.basinity.challengex.common.lifecycle.TimerColors;
import com.basinity.challengex.common.lifecycle.TimerPreferences;
import com.basinity.challengex.common.preset.PresetStore;
import com.basinity.challengex.common.text.StyledLine;
import com.basinity.challengex.core.engine.ChallengeRun;
import com.basinity.challengex.core.engine.RunState;
import com.basinity.challengex.core.preset.Preset;
import com.basinity.challengex.core.preset.PresetCodec;
import com.basinity.challengex.core.preset.PresetFormatException;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.paper.ChallengeXPaper;
import com.basinity.challengex.paper.lifecycle.RunController;
import com.basinity.challengex.paper.text.Lines;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * The {@code /challengex} command tree: the plugin's real admin surface for
 * loading challenges without a restart. {@code import} with no argument lists
 * the preset files in the config folder as clickable entries and prints the
 * folder path as click-to-copy text; {@code import <file>} parses that preset
 * and swaps it in as the active challenge; {@code reload} re-reads the last
 * imported file. Every mutating verb is gated; {@code info} and {@code config},
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
     * reload, start, reset, pause, resume) gates itself, while the read-only
     * {@code info} view and the personal {@code config} preferences stay open to
     * all players. That is what lets the clickable "view configuration" control
     * on the run-end message work for everyone in the run, not just the
     * operator, and what lets every player pick their own clock color or hide
     * the clock without needing permission.
     */
    public void register(Plugin plugin) {
        LifecycleEventManager<Plugin> lifecycle = plugin.getLifecycleManager();
        lifecycle.registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(Commands.literal("challengex")
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
                                .executes(this::info))
                        .build(),
                        "Compose and run your own Minecraft challenges"));
    }

    private int about(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage(Component.text(
                "ChallengeX: compose your own challenges from rules and modifiers"
                        + " to play alone or with your friends."
                        + " More than 500 million ways to play Minecraft.", NamedTextColor.GOLD));
        sender.sendMessage(builderLink());
        return 1;
    }

    private int listPresets(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        List<String> names = store.listPresetNames();
        if (names.isEmpty()) {
            sender.sendMessage(Component.text(
                    "No presets found. Drop a preset JSON into your presets folder,"
                            + " then run /challengex import <file>.", NamedTextColor.YELLOW));
            sender.sendMessage(folderCopyLine());
            sender.sendMessage(builderLink());
            return 1;
        }
        sender.sendMessage(Component.text("Presets (click to import):", NamedTextColor.GOLD));
        for (String name : names) {
            sender.sendMessage(importLink(name));
        }
        sender.sendMessage(folderCopyLine());
        sender.sendMessage(builderLink());
        return 1;
    }

    private int importNamed(CommandContext<CommandSourceStack> context) {
        return doImport(context.getSource(), StringArgumentType.getString(context, "file"));
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (activePresetName == null) {
            fail(source, "No preset imported yet. Use /challengex import <file> first.");
            return 0;
        }
        return doImport(source, activePresetName);
    }

    /** Reads, parses, and applies a preset; leaves the active challenge untouched on any failure. */
    private int doImport(CommandSourceStack source, String name) {
        Optional<String> json = store.read(name);
        if (json.isEmpty()) {
            fail(source, "No preset '" + name + "' in " + store.displayPath());
            return 0;
        }
        Preset preset;
        try {
            preset = codec.fromJson(json.get());
        } catch (PresetFormatException rejected) {
            fail(source, "Could not import '" + name + "':");
            for (String problem : rejected.problems()) {
                fail(source, "  - " + problem);
            }
            return 0;
        }
        ChallengeXPaper.instance().loadChallenge(preset.challenge());
        controller.onChallengeReplaced(source.getSender().getServer());
        activePresetName = name;
        source.getSender().sendMessage(Component.text(
                "Imported '" + preset.name() + "'. Run /challengex start to begin.", NamedTextColor.GREEN));
        return 1;
    }

    /** A run wrapping the empty starting challenge: nothing has been imported yet. */
    private static boolean noChallengeLoaded(ChallengeRun run) {
        return run == null || run.challenge().isEmpty();
    }

    private int start(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ChallengeRun run = ChallengeXPaper.instance().activeRun();
        if (noChallengeLoaded(run)) {
            fail(source, "No challenge loaded. Use /challengex import <file> first.");
            return 0;
        }
        switch (run.state()) {
            case RUNNING, PAUSED -> {
                fail(source, "A challenge is already running. Use /challengex reset first.");
                return 0;
            }
            case FINISHED -> {
                fail(source, "This run has finished. Use /challengex reset to play it again.");
                return 0;
            }
            case NOT_STARTED -> {
                controller.start();
                source.getSender().sendMessage(Component.text("Challenge started.", NamedTextColor.GREEN));
                return 1;
            }
        }
        return 0;
    }

    private int reset(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        controller.reset(source.getSender().getServer());
        source.getSender().sendMessage(Component.text(
                "Challenge reset. Run /challengex start to begin again.", NamedTextColor.GREEN));
        return 1;
    }

    private int pause(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ChallengeRun run = ChallengeXPaper.instance().activeRun();
        if (run == null || run.state() != RunState.RUNNING) {
            fail(source, "No running challenge to pause.");
            return 0;
        }
        controller.pause(source.getSender().getServer());
        source.getSender().sendMessage(Component.text("Challenge paused.", NamedTextColor.YELLOW));
        return 1;
    }

    private int resume(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ChallengeRun run = ChallengeXPaper.instance().activeRun();
        if (run == null || run.state() != RunState.PAUSED) {
            fail(source, "No paused challenge to resume.");
            return 0;
        }
        controller.resume(source.getSender().getServer());
        source.getSender().sendMessage(Component.text("Challenge resumed.", NamedTextColor.GREEN));
        return 1;
    }

    private int info(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        ChallengeRun run = ChallengeXPaper.instance().activeRun();
        if (noChallengeLoaded(run)) {
            sender.sendMessage(Component.text("No challenge loaded.", NamedTextColor.YELLOW));
            return 1;
        }
        for (StyledLine line : ChallengeSummary.describe(run.challenge(), activePresetName, run.state())) {
            sender.sendMessage(Lines.render(line));
        }
        return 1;
    }

    private int configShow(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = caller(context.getSource());
        if (player.isEmpty()) {
            return 0;
        }
        UUID id = player.get().getUniqueId();
        CommandSender sender = context.getSource().getSender();
        sender.sendMessage(Component.text("Your timer color: " + preferences.timerColor(id), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Your timer hidden: " + preferences.hideTimer(id), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("These are your own settings and affect nobody else."
                + " Change them with /challengex config timer_color <color>"
                + " and /challengex config hide_timer <true|false>.", NamedTextColor.GRAY));
        return 1;
    }

    private int timerColorShow(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = caller(context.getSource());
        if (player.isEmpty()) {
            return 0;
        }
        UUID id = player.get().getUniqueId();
        context.getSource().getSender().sendMessage(Component.text(
                "Your timer color: " + preferences.timerColor(id)
                        + ". Options: " + String.join(", ", TimerColors.names()), NamedTextColor.GOLD));
        return 1;
    }

    private int timerColorSet(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = caller(context.getSource());
        if (player.isEmpty()) {
            return 0;
        }
        String color = StringArgumentType.getString(context, "color").toLowerCase(Locale.ROOT);
        if (!preferences.setTimerColor(player.get().getUniqueId(), color)) {
            fail(context.getSource(), "Unknown color '" + color + "'. Options: "
                    + String.join(", ", TimerColors.names()));
            return 0;
        }
        context.getSource().getSender().sendMessage(
                Component.text("Timer color set to " + color + ".", NamedTextColor.GREEN));
        return 1;
    }

    private int hideTimerShow(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = caller(context.getSource());
        if (player.isEmpty()) {
            return 0;
        }
        context.getSource().getSender().sendMessage(Component.text(
                "Your timer hidden: " + preferences.hideTimer(player.get().getUniqueId()), NamedTextColor.GOLD));
        return 1;
    }

    private int hideTimerSet(CommandContext<CommandSourceStack> context) {
        Optional<Player> player = caller(context.getSource());
        if (player.isEmpty()) {
            return 0;
        }
        boolean hidden = BoolArgumentType.getBool(context, "hidden");
        preferences.setHideTimer(player.get().getUniqueId(), hidden);
        context.getSource().getSender().sendMessage(Component.text(
                hidden ? "Timer hidden." : "Timer shown.", NamedTextColor.GREEN));
        return 1;
    }

    /** The player who ran this, or empty (with a message) when the console did. */
    private Optional<Player> caller(CommandSourceStack source) {
        if (source.getSender() instanceof Player player) {
            return Optional.of(player);
        }
        fail(source, "These are per-player settings, so they have to be set by a player.");
        return Optional.empty();
    }

    private static void fail(CommandSourceStack source, String message) {
        source.getSender().sendMessage(Component.text(message, NamedTextColor.RED));
    }

    private CompletableFuture<Suggestions> suggestPresets(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String name : store.listPresetNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(typed)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestColors(CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder) {
        String typed = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String color : TimerColors.names()) {
            if (color.startsWith(typed)) {
                builder.suggest(color);
            }
        }
        return builder.buildFuture();
    }

    private Component importLink(String name) {
        return Component.text("  - " + name, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.runCommand("/challengex import " + name))
                .hoverEvent(HoverEvent.showText(Component.text("Import " + name)));
    }

    private Component folderCopyLine() {
        String path = store.displayPath();
        return Component.text("Presets folder: " + path, NamedTextColor.GRAY)
                .clickEvent(ClickEvent.copyToClipboard(path))
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy the folder path")));
    }

    private Component builderLink() {
        return Component.text("Build a challenge at " + BUILDER_URL, NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl(BUILDER_URL))
                .hoverEvent(HoverEvent.showText(Component.text("Open the ChallengeX builder")));
    }
}
