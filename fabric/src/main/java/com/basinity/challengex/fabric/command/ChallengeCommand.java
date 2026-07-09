package com.basinity.challengex.fabric.command;

import com.basinity.challengex.core.preset.Preset;
import com.basinity.challengex.core.preset.PresetCodec;
import com.basinity.challengex.core.preset.PresetFormatException;
import com.basinity.challengex.core.registry.CoreCatalog;
import com.basinity.challengex.fabric.ChallengeXFabric;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

/**
 * The {@code /challenge} command tree: the mod's real admin surface for loading
 * challenges without a server restart. {@code import} with no argument lists the
 * preset files in the config folder as clickable entries and prints the folder
 * path as click-to-copy text; {@code import <file>} parses that preset and swaps
 * it in as the active challenge; {@code reload} re-reads the last imported file.
 * The whole tree is gated on op level 2.
 *
 * <p>A rejected preset (unknown ids, bad scopes, malformed JSON) leaves the
 * active challenge untouched and prints every problem the codec found at once,
 * never a partial import.
 */
public final class ChallengeCommand {

    private final PresetStore store;
    private final PresetCodec codec;

    /** The last preset imported this session, so {@code reload} knows what to re-read. */
    private String activePresetName;

    public ChallengeCommand(PresetStore store) {
        this.store = store;
        this.codec = new PresetCodec(CoreCatalog.createRegistries());
    }

    public void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("challenge")
                        .requires(Perms.requireAdmin())
                        .then(Commands.literal("import")
                                .executes(this::listPresets)
                                .then(Commands.argument("file", StringArgumentType.greedyString())
                                        .suggests(this::suggestPresets)
                                        .executes(this::importNamed)))
                        .then(Commands.literal("reload")
                                .executes(this::reload))));
    }

    private int listPresets(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        List<String> names = store.listPresetNames();
        if (names.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "No presets found. Drop a preset JSON into your presets folder, then run /challenge import <file>.")
                    .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(this::folderCopyLine, false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Presets (click to import):").withStyle(ChatFormatting.GOLD), false);
        for (String name : names) {
            source.sendSuccess(() -> importLink(name), false);
        }
        source.sendSuccess(this::folderCopyLine, false);
        return 1;
    }

    private int importNamed(CommandContext<CommandSourceStack> context) {
        return doImport(context.getSource(), StringArgumentType.getString(context, "file"));
    }

    private int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (activePresetName == null) {
            source.sendFailure(Component.literal("No preset imported yet. Use /challenge import <file> first."));
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
        activePresetName = name;
        source.sendSuccess(() -> Component.literal("Imported '" + preset.name() + "' — now active.")
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
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
                .withClickEvent(new ClickEvent.RunCommand("/challenge import " + name))
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
}
