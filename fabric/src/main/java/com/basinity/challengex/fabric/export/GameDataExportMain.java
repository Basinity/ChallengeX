package com.basinity.challengex.fabric.export;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.SharedConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * Writes the web builder's game data: every id a suggesting parameter can name,
 * with its English display name from the game's own language file, so the site
 * can offer "Diamond Sword" and store {@code minecraft:diamond_sword} without a
 * player ever seeing an id. Driven by the {@code exportGameData} Gradle task,
 * the fabric sibling of {@code :core:exportCatalog}: the game itself stays the
 * single source of truth, nothing here is hand-written.
 *
 * <p>Like the catalog, it ships as a script assigning a global rather than a
 * bare {@code .json}, so the site still opens straight off disk over
 * {@code file://} and makes zero requests.
 *
 * <p>Each entry is {@code [id]} or {@code [id, name]}: the name is omitted when
 * the site can derive it from the id ("diamond_sword" reads back as "Diamond
 * Sword" with no help), which keeps the file small. The source keys mirror the
 * {@code suggests} values the core catalog declares. Suggestions never restrict
 * anything: an id absent here, modded or future, stays as exportable as ever.
 */
public final class GameDataExportMain {

    private static final Gson GSON = new Gson();

    /** The five real advancement trees; recipe unlocks are deliberately not advancements here. */
    private static final List<String> ADVANCEMENT_TREES = List.of("story", "nether", "end", "adventure", "husbandry");

    /** Living entities the mob list leaves out anyway; see the mob source below. */
    private static final Set<String> NOT_MOBS = Set.of("armor_stand", "player");

    private GameDataExportMain() {
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: GameDataExportMain <output-file> <game-version>");
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Language language = Language.getInstance();
        HolderLookup.Provider vanilla = VanillaRegistries.createLookup();
        // 26.2 binds item components through the data-component initializer
        // pipeline at server load, not at bootstrap; the food source reads
        // components, so run the same binding here.
        BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(vanilla)
                .forEach(DataComponentInitializers.PendingComponents::apply);

        Map<String, JsonArray> sources = new LinkedHashMap<>();
        sources.put("block", fromRegistry(BuiltInRegistries.BLOCK, language, block -> block.getDescriptionId()));
        sources.put("item", fromRegistry(BuiltInRegistries.ITEM, language, item -> item.getDescriptionId()));
        sources.put("entity", fromRegistry(BuiltInRegistries.ENTITY_TYPE, language, type -> type.getDescriptionId()));
        // The attribute filter catches every living entity, which technically
        // includes armor stands and players; neither reads as a mob (players
        // have their own kill-player trigger), so both stay out of the list.
        // Giant and illusioner stay in: unobtainable in survival, but the
        // spawn-mob effect can genuinely spawn them.
        sources.put("mob", entries(BuiltInRegistries.ENTITY_TYPE.stream()
                .filter(DefaultAttributes::hasSupplier)
                .filter(type -> !NOT_MOBS.contains(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath()))
                .map(type -> named(BuiltInRegistries.ENTITY_TYPE.getKey(type), langName(language, type.getDescriptionId())))));
        // Exactly the set the food-eaten trigger fires for: items carrying the
        // game's own food component, not a hand-picked list.
        sources.put("food", entries(BuiltInRegistries.ITEM.stream()
                .filter(item -> item.components().has(DataComponents.FOOD))
                .map(item -> named(BuiltInRegistries.ITEM.getKey(item), langName(language, item.getDescriptionId())))));
        sources.put("effect", fromRegistry(BuiltInRegistries.MOB_EFFECT, language, effect -> effect.getDescriptionId()));
        sources.put("sound", fromRegistry(BuiltInRegistries.SOUND_EVENT, language, sound -> null));
        sources.put("container", fromRegistry(BuiltInRegistries.MENU, language, menu -> null));
        sources.put("biome", fromLookup(vanilla, Registries.BIOME, id ->
                langName(language, "biome." + id.getNamespace() + "." + id.getPath())));
        sources.put("enchantment", fromLookup(vanilla, Registries.ENCHANTMENT, id ->
                langName(language, "enchantment." + id.getNamespace() + "." + id.getPath())));
        sources.put("damage_type", fromLookup(vanilla, Registries.DAMAGE_TYPE, id -> null));
        sources.put("dimension", entries(Stream.of("overworld", "the_nether", "the_end")
                .map(path -> named(Identifier.withDefaultNamespace(path), null))));
        sources.put("advancement", advancements(language));
        sources.put("weather", keywords("clear", "rain", "thunder"));
        sources.put("time", keywords("day", "noon", "night", "midnight"));
        sources.put("effect_kind", keywords("negative", "positive", "any"));
        // Keyword sets are bare vocabulary, not registry ids: the site must not
        // put a minecraft: namespace back on them when a preset stores the value.
        List<String> keywordSources = List.of("weather", "time", "effect_kind");

        Path target = Path.of(args[0]);
        Files.createDirectories(target.toAbsolutePath().getParent());
        Files.writeString(target, render(args[1], sources, keywordSources), StandardCharsets.UTF_8);
        System.out.println("Wrote game data to " + target.toAbsolutePath());
    }

    /* ---------- the sources ---------- */

    private static <T> JsonArray fromRegistry(Registry<T> registry, Language language, Function<T, String> descriptionId) {
        return entries(registry.stream().map(value -> {
            String key = descriptionId.apply(value);
            return named(registry.getKey(value), key == null ? null : langName(language, key));
        }));
    }

    private static JsonArray fromLookup(HolderLookup.Provider vanilla, net.minecraft.resources.ResourceKey<? extends Registry<?>> registry, Function<Identifier, String> name) {
        return entries(vanilla.lookupOrThrow(registry).listElements()
                .map(holder -> holder.key().identifier())
                .map(id -> named(id, name.apply(id))));
    }

    /**
     * The advancements of the five real trees, read out of the game jar's own
     * data files, titled by their translation key ("Diamonds!", not
     * "mine_diamond"). Vanilla ships every advancement as JSON inside the jar,
     * so the jar the build already compiles against is the source.
     */
    private static JsonArray advancements(Language language) throws IOException, URISyntaxException {
        Path jar = Path.of(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<Entry> found = new ArrayList<>();
        try (FileSystem files = FileSystems.newFileSystem(jar)) {
            for (String tree : ADVANCEMENT_TREES) {
                Path root = files.getPath("data", "minecraft", "advancement", tree);
                try (Stream<Path> walk = Files.walk(root)) {
                    for (Path file : walk.filter(path -> path.toString().endsWith(".json")).toList()) {
                        String path = tree + "/" + root.relativize(file).toString()
                                .replace(files.getSeparator(), "/")
                                .replaceAll("\\.json$", "");
                        found.add(new Entry(Identifier.withDefaultNamespace(path), title(file, language)));
                    }
                }
            }
        }
        return entries(found.stream());
    }

    /** An advancement's display title, or null (derive from the id) when it has no display block. */
    private static String title(Path file, Language language) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("display")) {
                return null;
            }
            JsonObject titleObject = root.getAsJsonObject("display").getAsJsonObject("title");
            if (titleObject == null || !titleObject.has("translate")) {
                return null;
            }
            String key = titleObject.get("translate").getAsString();
            return language.has(key) ? language.getOrDefault(key) : null;
        }
    }

    private static JsonArray keywords(String... values) {
        return entries(Stream.of(values).map(value -> new Entry(Identifier.withDefaultNamespace(value), null)));
    }

    /* ---------- entry shaping ---------- */

    private record Entry(Identifier id, String name) {
    }

    private static Entry named(Identifier id, String name) {
        return new Entry(id, name);
    }

    /** The language file's name for a key, or null (derive from the id) when it has none. */
    private static String langName(Language language, String key) {
        return language.has(key) ? language.getOrDefault(key) : null;
    }

    /**
     * Sorted, minified entries: the id loses its {@code minecraft:} namespace
     * (the site puts it back on export) and the name is dropped whenever it
     * matches what the site derives from the id on its own.
     */
    private static JsonArray entries(Stream<Entry> stream) {
        JsonArray array = new JsonArray();
        stream.sorted((a, b) -> a.id().compareTo(b.id())).forEach(entry -> {
            String id = entry.id().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)
                    ? entry.id().getPath()
                    : entry.id().toString();
            JsonArray row = new JsonArray();
            row.add(id);
            if (entry.name() != null && !entry.name().equals(derivedName(id))) {
                row.add(entry.name());
            }
            array.add(row);
        });
        return array;
    }

    /**
     * The site's fallback display name for an id, mirrored exactly by
     * {@code derivedName} in {@code web/assets/js/suggest.js}: the part
     * after the last slash, dots and underscores to spaces, each word
     * capitalized. The two must stay identical or omitted names would render
     * differently than the export assumed.
     */
    private static String derivedName(String id) {
        String tail = id.substring(id.lastIndexOf('/') + 1);
        String[] words = tail.replace('.', ' ').replace('_', ' ').split(" ");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return out.toString();
    }

    /* ---------- output ---------- */

    private static String render(String gameVersion, Map<String, JsonArray> sources, List<String> keywordSources) {
        StringBuilder out = new StringBuilder();
        out.append("// Generated by the :fabric:exportGameData Gradle task from the game registries.\n");
        out.append("// Do not edit by hand: rerun the task after a game-version bump.\n");
        out.append("window.CX_GAMEDATA = {\n");
        out.append("\"gameVersion\": ").append(GSON.toJson(gameVersion)).append(",\n");
        out.append("\"keywords\": ").append(GSON.toJson(keywordSources)).append(",\n");
        out.append("\"sources\": {\n");
        int index = 0;
        for (Map.Entry<String, JsonArray> source : sources.entrySet()) {
            out.append(GSON.toJson(source.getKey())).append(": ").append(GSON.toJson(source.getValue()));
            out.append(++index < sources.size() ? ",\n" : "\n");
        }
        out.append("}\n};\n");
        return out.toString();
    }
}
